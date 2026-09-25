package com.bomberman.server.game;

import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.GameStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/** Core authoritative game aggregate. All mutations are serialized by its room game loop. */
public final class BombermanGame {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;
    public static final Duration BOMB_FUSE = Duration.ofSeconds(3);
    public static final Duration EXPLOSION_DURATION = Duration.ofMillis(500);

    private final GameMap gameMap;
    private final Map<Long, BomberPlayer> players = new LinkedHashMap<>();
    private final Map<Position, Bomb> bombs = new LinkedHashMap<>();
    private final List<Explosion> activeExplosions = new ArrayList<>();
    private final Lock stateLock = new ReentrantLock();

    private GameOutcome outcome;

    public BombermanGame(
            GameMap gameMap,
            Collection<BomberPlayer> players,
            Collection<Bomb> bombs
    ) {
        this.gameMap = Objects.requireNonNull(gameMap, "gameMap must not be null");
        Objects.requireNonNull(players, "players must not be null");
        Objects.requireNonNull(bombs, "bombs must not be null");
        if (players.size() < MIN_PLAYERS || players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("A game requires between 2 and 4 players");
        }

        for (BomberPlayer player : players) {
            validateInitialPlayer(player);
            if (this.players.putIfAbsent(player.getUserId(), player) != null) {
                throw new IllegalArgumentException("Duplicate player userId: " + player.getUserId());
            }
        }
        ensureUniquePlayerPositions();

        for (Bomb bomb : bombs) {
            if (!gameMap.isWalkable(bomb.position())) {
                throw new IllegalArgumentException("Bomb must be on an empty map tile");
            }
            if (this.bombs.putIfAbsent(bomb.position(), bomb) != null) {
                throw new IllegalArgumentException("Only one bomb may occupy a position");
            }
        }
    }

    public BombermanGame(GameMap gameMap, Collection<BomberPlayer> players) {
        this(gameMap, players, List.of());
    }

    public static BombermanGame createDefault(List<GameParticipant> participants) {
        Objects.requireNonNull(participants, "participants must not be null");
        if (participants.size() < MIN_PLAYERS || participants.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("A game requires between 2 and 4 players");
        }

        GameMap map = GameMap.createDefault();
        List<Position> spawns = map.getSpawnPositions();
        List<BomberPlayer> players = java.util.stream.IntStream.range(0, participants.size())
                .mapToObj(index -> new BomberPlayer(
                        participants.get(index).userId(),
                        participants.get(index).username(),
                        spawns.get(index)
                ))
                .toList();
        return new BombermanGame(map, players);
    }

    /**
     * Moves one tile if authoritative collision checks allow it. A bomb blocks
     * entry immediately, while a player standing on a newly placed bomb may move away.
     */
    public MoveResult movePlayer(long userId, Direction direction) {
        Objects.requireNonNull(direction, "direction must not be null");
        stateLock.lock();
        try {
            if (outcome != null) {
                return MoveResult.GAME_OVER;
            }
            BomberPlayer player = players.get(userId);
            if (player == null) {
                return MoveResult.PLAYER_NOT_FOUND;
            }
            if (!player.isAlive()) {
                return MoveResult.PLAYER_DEAD;
            }

            Position destination = player.getPosition().move(direction);
            if (!gameMap.isInside(destination)) {
                return MoveResult.OUT_OF_BOUNDS;
            }
            if (!gameMap.isWalkable(destination)) {
                return MoveResult.BLOCKED_BY_WALL;
            }
            if (bombs.containsKey(destination)) {
                return MoveResult.BLOCKED_BY_BOMB;
            }
            boolean occupiedByPlayer = players.values().stream()
                    .anyMatch(other -> other.getUserId() != userId
                            && other.isAlive()
                            && other.getPosition().equals(destination));
            if (occupiedByPlayer) {
                return MoveResult.BLOCKED_BY_PLAYER;
            }

            player.moveTo(destination);
            return MoveResult.MOVED;
        } finally {
            stateLock.unlock();
        }
    }

    public PlaceBombResult placeBomb(long userId, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        stateLock.lock();
        try {
            if (outcome != null) {
                return PlaceBombResult.GAME_OVER;
            }
            BomberPlayer player = players.get(userId);
            if (player == null) {
                return PlaceBombResult.PLAYER_NOT_FOUND;
            }
            if (!player.isAlive()) {
                return PlaceBombResult.PLAYER_DEAD;
            }
            if (bombs.containsKey(player.getPosition())) {
                return PlaceBombResult.BOMB_ALREADY_PRESENT;
            }
            if (!player.canPlaceBomb()) {
                return PlaceBombResult.BOMB_CAPACITY_REACHED;
            }

            Bomb bomb = Bomb.scheduled(
                    userId,
                    player.getPosition(),
                    player.getBombRange(),
                    now,
                    now.plus(BOMB_FUSE)
            );
            bombs.put(bomb.position(), bomb);
            player.bombPlaced();
            return PlaceBombResult.PLACED;
        } finally {
            stateLock.unlock();
        }
    }

    /** Marks a disconnected participant dead. Called only by the room game-loop thread. */
    public GameOutcome disconnectPlayer(long userId) {
        stateLock.lock();
        try {
            BomberPlayer player = players.get(userId);
            if (player != null && player.isAlive()) {
                player.kill();
                outcome = determineOutcome();
            }
            return outcome;
        } finally {
            stateLock.unlock();
        }
    }

    public GameTickResult tick(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        stateLock.lock();
        try {
            activeExplosions.removeIf(explosion -> !explosion.expiresAt().isAfter(now));
            if (outcome != null) {
                return emptyTick(outcome);
            }

            List<Explosion> newExplosions = new ArrayList<>();
            Set<Position> destroyedWalls = new LinkedHashSet<>();
            Set<Long> diedPlayerIds = new LinkedHashSet<>();
            Deque<Bomb> detonationQueue = new ArrayDeque<>();
            Set<String> queuedBombIds = new LinkedHashSet<>();

            bombs.values().stream()
                    .filter(bomb -> !bomb.detonateAt().isAfter(now))
                    .forEach(bomb -> enqueueBomb(bomb, detonationQueue, queuedBombIds));

            while (!detonationQueue.isEmpty()) {
                Bomb bomb = detonationQueue.removeFirst();
                Bomb currentBomb = bombs.get(bomb.position());
                if (currentBomb == null || !currentBomb.bombId().equals(bomb.bombId())) {
                    continue;
                }

                bombs.remove(bomb.position());
                BomberPlayer owner = players.get(bomb.ownerUserId());
                if (owner != null) {
                    owner.bombExploded();
                }

                Explosion explosion = createExplosion(
                        bomb,
                        now,
                        detonationQueue,
                        queuedBombIds,
                        destroyedWalls
                );
                newExplosions.add(explosion);
                activeExplosions.add(explosion);
                killPlayersIn(explosion, diedPlayerIds);
            }

            if (!diedPlayerIds.isEmpty()) {
                outcome = determineOutcome();
            }
            return new GameTickResult(newExplosions, destroyedWalls, diedPlayerIds, outcome);
        } finally {
            stateLock.unlock();
        }
    }

    public GameMap getGameMap() {
        return gameMap;
    }

    public BomberPlayer getPlayer(long userId) {
        stateLock.lock();
        try {
            return players.get(userId);
        } finally {
            stateLock.unlock();
        }
    }

    public int getBombCount() {
        stateLock.lock();
        try {
            return bombs.size();
        } finally {
            stateLock.unlock();
        }
    }

    public List<Explosion> getActiveExplosions() {
        stateLock.lock();
        try {
            return List.copyOf(activeExplosions);
        } finally {
            stateLock.unlock();
        }
    }

    public GameOutcome getOutcome() {
        stateLock.lock();
        try {
            return outcome;
        } finally {
            stateLock.unlock();
        }
    }

    public BombermanSnapshot createSnapshot(long tick, Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        stateLock.lock();
        try {
            List<BombermanSnapshot.PlayerSnapshot> playerSnapshots = players.values().stream()
                    .map(player -> new BombermanSnapshot.PlayerSnapshot(
                            player.getUserId(),
                            player.getUsername(),
                            player.getPosition(),
                            player.isAlive(),
                            player.getBombCapacity(),
                            player.getActiveBombs(),
                            player.getBombRange()
                    ))
                    .toList();
            List<BombermanSnapshot.BombSnapshot> bombSnapshots = bombs.values().stream()
                    .map(bomb -> new BombermanSnapshot.BombSnapshot(
                            bomb.bombId(),
                            bomb.ownerUserId(),
                            bomb.position(),
                            bomb.blastRange(),
                            remainingMillis(now, bomb.detonateAt())
                    ))
                    .toList();
            List<BombermanSnapshot.ExplosionSnapshot> explosionSnapshots = activeExplosions.stream()
                    .map(explosion -> new BombermanSnapshot.ExplosionSnapshot(
                            explosion.origin(),
                            List.copyOf(explosion.affectedPositions()),
                            remainingMillis(now, explosion.expiresAt())
                    ))
                    .toList();
            int remainingPlayers = (int) players.values().stream()
                    .filter(BomberPlayer::isAlive)
                    .count();

            return new BombermanSnapshot(
                    tick,
                    outcome == null ? GameStatus.RUNNING : GameStatus.FINISHED,
                    gameMap.snapshotTiles(),
                    playerSnapshots,
                    bombSnapshots,
                    explosionSnapshots,
                    remainingPlayers
            );
        } finally {
            stateLock.unlock();
        }
    }

    private Explosion createExplosion(
            Bomb bomb,
            Instant now,
            Deque<Bomb> detonationQueue,
            Set<String> queuedBombIds,
            Set<Position> destroyedWalls
    ) {
        Set<Position> affected = new LinkedHashSet<>();
        affected.add(bomb.position());

        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= bomb.blastRange(); distance++) {
                Position position = move(bomb.position(), direction, distance);
                if (!gameMap.isInside(position)) {
                    break;
                }

                Tile tile = gameMap.getTile(position);
                if (tile == Tile.HARD_WALL) {
                    break;
                }

                affected.add(position);
                if (tile == Tile.BREAKABLE_WALL) {
                    gameMap.destroyBreakableWall(position);
                    destroyedWalls.add(position);
                    break;
                }

                Bomb chainedBomb = bombs.get(position);
                if (chainedBomb != null) {
                    enqueueBomb(chainedBomb, detonationQueue, queuedBombIds);
                    break;
                }
            }
        }

        return new Explosion(
                bomb.position(),
                affected,
                now,
                now.plus(EXPLOSION_DURATION)
        );
    }

    private void enqueueBomb(Bomb bomb, Deque<Bomb> queue, Set<String> queuedBombIds) {
        if (queuedBombIds.add(bomb.bombId())) {
            queue.addLast(bomb);
        }
    }

    private void killPlayersIn(Explosion explosion, Set<Long> diedPlayerIds) {
        for (BomberPlayer player : players.values()) {
            if (player.isAlive() && explosion.affectedPositions().contains(player.getPosition())) {
                player.kill();
                diedPlayerIds.add(player.getUserId());
            }
        }
    }

    private GameOutcome determineOutcome() {
        List<BomberPlayer> alivePlayers = players.values().stream()
                .filter(BomberPlayer::isAlive)
                .toList();
        if (alivePlayers.size() == 1) {
            return GameOutcome.win(alivePlayers.getFirst().getUserId());
        }
        if (alivePlayers.isEmpty()) {
            return GameOutcome.draw();
        }
        return null;
    }

    private Position move(Position origin, Direction direction, int distance) {
        Position position = origin;
        for (int step = 0; step < distance; step++) {
            position = position.move(direction);
        }
        return position;
    }

    private GameTickResult emptyTick(GameOutcome existingOutcome) {
        return new GameTickResult(List.of(), Set.of(), Set.of(), existingOutcome);
    }

    private long remainingMillis(Instant now, Instant end) {
        return Math.max(0, Duration.between(now, end).toMillis());
    }

    private void validateInitialPlayer(BomberPlayer player) {
        Objects.requireNonNull(player, "players must not contain null");
        if (!gameMap.isWalkable(player.getPosition())) {
            throw new IllegalArgumentException("Player must start on an empty map tile");
        }
    }

    private void ensureUniquePlayerPositions() {
        long distinctPositions = players.values().stream()
                .map(BomberPlayer::getPosition)
                .distinct()
                .count();
        if (distinctPositions != players.size()) {
            throw new IllegalArgumentException("Players cannot share an initial position");
        }
    }
}
