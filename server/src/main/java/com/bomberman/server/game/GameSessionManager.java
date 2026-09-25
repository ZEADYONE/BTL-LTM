package com.bomberman.server.game;

import com.bomberman.common.enums.PlayerStatus;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameOverPlayerDto;
import com.bomberman.common.message.NetworkMessage;
import com.bomberman.server.lobby.LobbyService;
import com.bomberman.server.match.MatchPersistenceService;
import com.bomberman.server.match.MatchScoring;
import com.bomberman.server.network.ClientSession;
import com.bomberman.server.network.ConnectionManager;
import com.bomberman.server.room.RoomManager;
import com.bomberman.server.room.RoomSnapshot;
import com.bomberman.server.user.OnlineUserRegistry;
import jakarta.annotation.PreDestroy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Creates and routes commands to the authoritative loop belonging to each room. */
@Component
public class GameSessionManager {

    private static final Logger log = LoggerFactory.getLogger(GameSessionManager.class);

    private final ConcurrentMap<String, RoomGameLoop> loopsByRoom = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> roomByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<String>> sessionIdsByRoom = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, MatchContext> matchContexts = new ConcurrentHashMap<>();

    private final RoomManager roomManager;
    private final OnlineUserRegistry onlineUserRegistry;
    private final LobbyService lobbyService;
    private final ConnectionManager connectionManager;
    private final GameStateMapper gameStateMapper;
    private final ObjectMapper objectMapper;
    private final MatchPersistenceService matchPersistenceService;
    private final Clock clock = Clock.systemUTC();

    public GameSessionManager(
            RoomManager roomManager,
            OnlineUserRegistry onlineUserRegistry,
            LobbyService lobbyService,
            ConnectionManager connectionManager,
            GameStateMapper gameStateMapper,
            ObjectMapper objectMapper,
            MatchPersistenceService matchPersistenceService
    ) {
        this.roomManager = roomManager;
        this.onlineUserRegistry = onlineUserRegistry;
        this.lobbyService = lobbyService;
        this.connectionManager = connectionManager;
        this.gameStateMapper = gameStateMapper;
        this.objectMapper = objectMapper;
        this.matchPersistenceService = matchPersistenceService;
    }

    public boolean startGame(RoomSnapshot room) {
        Instant startedAt = clock.instant();
        List<GameParticipant> participants = room.players().stream()
                .map(player -> new GameParticipant(player.userId(), player.username()))
                .toList();
        BombermanGame game = BombermanGame.createDefault(participants);
        RoomGameLoop loop = new RoomGameLoop(
                room.roomId(),
                game,
                clock,
                new GameLoopListener() {
                    @Override
                    public void onGameState(String roomId, BombermanSnapshot snapshot) {
                        broadcastGameState(roomId, snapshot);
                    }

                    @Override
                    public void onGameOver(
                            String roomId,
                            BombermanGame game,
                            GameOutcome outcome
                    ) {
                        handleGameOver(roomId, game, outcome);
                    }
                }
        );
        if (loopsByRoom.putIfAbsent(room.roomId(), loop) != null) {
            loop.close();
            return false;
        }

        room.players().forEach(player -> roomByUser.put(player.userId(), room.roomId()));
        sessionIdsByRoom.put(
                room.roomId(),
                room.players().stream().map(player -> player.sessionId()).toList()
        );
        matchContexts.put(room.roomId(), new MatchContext(startedAt, participants));
        loop.start();
        return true;
    }

    public GameCommandEnqueueResult enqueueMove(
            long userId,
            com.bomberman.common.enums.Direction direction
    ) {
        return enqueue(userId, new MoveGameCommand(userId, direction));
    }

    public GameCommandEnqueueResult enqueuePlaceBomb(long userId) {
        return enqueue(userId, new PlaceBombGameCommand(userId));
    }

    public GameCommandEnqueueResult enqueueDisconnect(long userId) {
        return enqueue(userId, new DisconnectGameCommand(userId));
    }

    public Optional<BombermanGame> findGame(String roomId) {
        return Optional.ofNullable(loopsByRoom.get(roomId)).map(RoomGameLoop::getGame);
    }

    public Optional<Boolean> isPlayerAlive(long userId) {
        RoomGameLoop loop = findLoopByUser(userId);
        if (loop == null) {
            return Optional.empty();
        }
        BomberPlayer player = loop.getGame().getPlayer(userId);
        return player == null ? Optional.empty() : Optional.of(player.isAlive());
    }

    @PreDestroy
    public void shutdown() {
        loopsByRoom.values().forEach(RoomGameLoop::close);
        loopsByRoom.clear();
        roomByUser.clear();
        sessionIdsByRoom.clear();
        matchContexts.clear();
    }

    private RoomGameLoop findLoopByUser(long userId) {
        String roomId = roomByUser.get(userId);
        return roomId == null ? null : loopsByRoom.get(roomId);
    }

    private GameCommandEnqueueResult enqueue(long userId, GameCommand command) {
        RoomGameLoop loop = findLoopByUser(userId);
        if (loop == null || !loop.isRunning()) {
            return GameCommandEnqueueResult.GAME_NOT_RUNNING;
        }
        return loop.enqueue(command)
                ? GameCommandEnqueueResult.ACCEPTED
                : GameCommandEnqueueResult.QUEUE_FULL;
    }

    private void handleGameOver(String roomId, BombermanGame game, GameOutcome outcome) {
        MatchContext context = matchContexts.remove(roomId);
        if (context != null) {
            try {
                matchPersistenceService.recordCompletedMatch(
                        roomId,
                        context.startedAt(),
                        clock.instant(),
                        context.participants(),
                        outcome
                );
            } catch (RuntimeException exception) {
                log.error("Failed to persist completed match for room {}", roomId, exception);
            }
        }
        broadcastGameOver(roomId, context, outcome);
        log.info(
                "Game over: roomId={}, result={}, winnerUserId={}",
                roomId,
                outcome.result(),
                outcome.winnerUserId()
        );
        loopsByRoom.remove(roomId);
        roomByUser.entrySet().stream()
                .filter(entry -> entry.getValue().equals(roomId))
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(userId -> {
                    roomByUser.remove(userId, roomId);
                    onlineUserRegistry.updateStatus(userId, PlayerStatus.IN_ROOM);
                });
        roomManager.finishGame(roomId);
        sessionIdsByRoom.remove(roomId);
        lobbyService.broadcastLobbyUpdates();
    }

    private void broadcastGameState(String roomId, BombermanSnapshot snapshot) {
        GameStateDto state = gameStateMapper.toDto(snapshot);
        NetworkMessage message = new NetworkMessage(
                MessageType.GAME_STATE,
                null,
                objectMapper.valueToTree(state)
        );
        for (String sessionId : sessionIdsByRoom.getOrDefault(roomId, List.of())) {
            connectionManager.find(sessionId).ifPresent(session -> send(session, message));
        }
    }

    private void broadcastGameOver(
            String roomId,
            MatchContext context,
            GameOutcome outcome
    ) {
        if (context == null) {
            return;
        }
        GameOverDto gameOver = new GameOverDto(
                outcome.winnerUserId(),
                outcome.result(),
                context.participants().stream()
                        .map(participant -> {
                            var result = MatchScoring.playerResult(
                                    participant.userId(),
                                    outcome.result(),
                                    outcome.winnerUserId()
                            );
                            return new GameOverPlayerDto(
                                    participant.userId(),
                                    result,
                                    MatchScoring.scoreUnits(result)
                            );
                        })
                        .toList()
        );
        NetworkMessage message = new NetworkMessage(
                MessageType.GAME_OVER,
                null,
                objectMapper.valueToTree(gameOver)
        );
        for (String sessionId : sessionIdsByRoom.getOrDefault(roomId, List.of())) {
            connectionManager.find(sessionId).ifPresent(session -> send(session, message));
        }
    }

    private void send(ClientSession session, NetworkMessage message) {
        try {
            session.send(message);
        } catch (IOException exception) {
            log.info("Failed to send game message to session {}; closing it", session.getSessionId());
            session.close();
        }
    }

    private record MatchContext(Instant startedAt, List<GameParticipant> participants) {

        private MatchContext {
            participants = List.copyOf(participants);
        }
    }
}
