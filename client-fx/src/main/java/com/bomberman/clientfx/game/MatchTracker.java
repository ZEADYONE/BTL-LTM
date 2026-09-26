package com.bomberman.clientfx.game;

import com.bomberman.common.dto.BombStateDto;
import com.bomberman.common.dto.ExplosionStateDto;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameOverPlayerDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.PositionDto;
import com.bomberman.common.enums.GameResult;
import com.bomberman.common.enums.TileType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/** Derives client-side visual events and stable slot assignments from authoritative snapshots. */
public final class MatchTracker {

    /** Stable data consumed by the phase-6 result table. */
    public record RankedPlayer(
            long userId,
            String username,
            int slot,
            int rank,
            GameResult result,
            int scoreEarnedUnits
    ) {
    }

    public record FrameEvents(
            boolean newMatch,
            List<PositionDto> brokenCrates,
            List<ExplosionStateDto> newExplosions,
            List<BombStateDto> newBombs,
            List<Long> movedPlayers,
            List<Long> eliminatedPlayers
    ) {
        private static FrameEvents empty() {
            return new FrameEvents(false, List.of(), List.of(), List.of(), List.of(), List.of());
        }
    }

    private final Map<Long, Integer> slots = new LinkedHashMap<>();
    private final Map<Long, String> names = new LinkedHashMap<>();
    private final Map<Long, Long> eliminationTicks = new LinkedHashMap<>();
    private final Set<String> seenBombIds = new HashSet<>();
    private GameStateDto previous;
    private FrameEvents latestEvents = FrameEvents.empty();

    public FrameEvents accept(GameStateDto snapshot) {
        boolean newMatch = previous == null || snapshot.tick() < previous.tick();
        if (newMatch) {
            clearDerivedState();
            for (int slot = 0; slot < snapshot.players().size(); slot++) {
                GamePlayerStateDto player = snapshot.players().get(slot);
                slots.put(player.userId(), slot);
                names.put(player.userId(), player.username());
                if (!player.alive()) {
                    eliminationTicks.put(player.userId(), snapshot.tick());
                }
            }
        }

        List<PositionDto> brokenCrates = previous == null ? List.of() : brokenCrates(previous, snapshot);
        List<ExplosionStateDto> newExplosions = newExplosions(previous, snapshot);
        List<BombStateDto> newBombs = snapshot.bombs().stream()
                .filter(bomb -> seenBombIds.add(bomb.bombId()))
                .toList();
        List<Long> movedPlayers = previous == null ? List.of() : movedPlayers(previous, snapshot);
        List<Long> eliminatedPlayers = previous == null ? List.of() : eliminatedPlayers(previous, snapshot);
        eliminatedPlayers.forEach(userId -> eliminationTicks.putIfAbsent(userId, snapshot.tick()));
        snapshot.players().forEach(player -> names.put(player.userId(), player.username()));

        previous = snapshot;
        latestEvents = new FrameEvents(
                newMatch,
                List.copyOf(brokenCrates),
                List.copyOf(newExplosions),
                List.copyOf(newBombs),
                List.copyOf(movedPlayers),
                List.copyOf(eliminatedPlayers)
        );
        return latestEvents;
    }

    public void reset() {
        previous = null;
        clearDerivedState();
        latestEvents = FrameEvents.empty();
    }

    public OptionalInt slotFor(long userId) {
        Integer slot = slots.get(userId);
        return slot == null ? OptionalInt.empty() : OptionalInt.of(slot);
    }

    public Map<Long, Integer> slots() {
        return Map.copyOf(slots);
    }

    public Map<Long, Long> eliminationTicks() {
        return Map.copyOf(eliminationTicks);
    }

    public Map<Long, String> names() {
        return Map.copyOf(names);
    }

    public FrameEvents latestEvents() {
        return latestEvents;
    }

    /**
     * Ranks the final DTO using elimination ticks when every loss was observed. If snapshots were
     * missed, falls back deterministically to WIN/DRAW/LOSS and then the stable slot order.
     */
    public List<RankedPlayer> rank(GameOverDto gameOver) {
        List<GameOverPlayerDto> players = gameOver.players();
        boolean allDraw = !players.isEmpty() && players.stream().allMatch(player -> player.result() == GameResult.DRAW);
        boolean completeEliminationData = players.stream()
                .filter(player -> player.result() == GameResult.LOSS)
                .allMatch(player -> eliminationTicks.containsKey(player.userId()));

        Comparator<GameOverPlayerDto> comparator = Comparator
                .comparingInt((GameOverPlayerDto player) -> resultOrder(player.result()));
        if (completeEliminationData) {
            comparator = comparator.thenComparing(
                    (GameOverPlayerDto player) -> eliminationTicks.getOrDefault(player.userId(), Long.MIN_VALUE),
                    Comparator.reverseOrder()
            );
        }
        comparator = comparator.thenComparingInt(player -> slots.getOrDefault(player.userId(), Integer.MAX_VALUE));
        List<GameOverPlayerDto> ordered = players.stream().sorted(comparator).toList();

        List<RankedPlayer> result = new ArrayList<>(ordered.size());
        int previousRank = 0;
        GameOverPlayerDto previous = null;
        for (int index = 0; index < ordered.size(); index++) {
            GameOverPlayerDto player = ordered.get(index);
            int rank;
            if (allDraw) {
                rank = 1;
            } else if (previous != null && completeEliminationData && tied(previous, player)) {
                rank = previousRank;
            } else {
                rank = index + 1;
            }
            int slot = slots.getOrDefault(player.userId(), index);
            result.add(new RankedPlayer(
                    player.userId(),
                    names.getOrDefault(player.userId(), "Player " + (slot + 1)),
                    slot,
                    rank,
                    player.result(),
                    player.scoreEarnedUnits()
            ));
            previous = player;
            previousRank = rank;
        }
        return List.copyOf(result);
    }

    private void clearDerivedState() {
        slots.clear();
        names.clear();
        eliminationTicks.clear();
        seenBombIds.clear();
    }

    private boolean tied(GameOverPlayerDto left, GameOverPlayerDto right) {
        if (left.result() != right.result()) {
            return false;
        }
        if (left.result() == GameResult.DRAW) {
            return true;
        }
        if (left.result() == GameResult.WIN) {
            return true;
        }
        return eliminationTicks.get(left.userId()).equals(eliminationTicks.get(right.userId()));
    }

    private static int resultOrder(GameResult result) {
        return switch (result) {
            case WIN -> 0;
            case DRAW -> 1;
            case LOSS -> 2;
        };
    }

    private static List<PositionDto> brokenCrates(GameStateDto before, GameStateDto after) {
        List<PositionDto> result = new ArrayList<>();
        int rows = Math.min(before.map().size(), after.map().size());
        for (int row = 0; row < rows; row++) {
            int columns = Math.min(before.map().get(row).size(), after.map().get(row).size());
            for (int column = 0; column < columns; column++) {
                if (before.map().get(row).get(column) == TileType.BREAKABLE_WALL
                        && after.map().get(row).get(column) == TileType.EMPTY) {
                    result.add(new PositionDto(column, row));
                }
            }
        }
        return result;
    }

    private static List<ExplosionStateDto> newExplosions(GameStateDto before, GameStateDto after) {
        Set<PositionDto> oldOrigins = before == null
                ? Set.of()
                : before.explosions().stream().map(ExplosionStateDto::origin).collect(java.util.stream.Collectors.toSet());
        return after.explosions().stream()
                .filter(explosion -> !oldOrigins.contains(explosion.origin()))
                .toList();
    }

    private static List<Long> movedPlayers(GameStateDto before, GameStateDto after) {
        Map<Long, PositionDto> oldPositions = new HashMap<>();
        before.players().forEach(player -> oldPositions.put(player.userId(), player.position()));
        return after.players().stream()
                .filter(player -> oldPositions.containsKey(player.userId()))
                .filter(player -> !player.position().equals(oldPositions.get(player.userId())))
                .map(GamePlayerStateDto::userId)
                .toList();
    }

    private static List<Long> eliminatedPlayers(GameStateDto before, GameStateDto after) {
        Map<Long, Boolean> oldAlive = new HashMap<>();
        before.players().forEach(player -> oldAlive.put(player.userId(), player.alive()));
        return after.players().stream()
                .filter(player -> Boolean.TRUE.equals(oldAlive.get(player.userId())) && !player.alive())
                .map(GamePlayerStateDto::userId)
                .toList();
    }
}
