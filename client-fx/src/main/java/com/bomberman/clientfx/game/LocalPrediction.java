package com.bomberman.clientfx.game;

import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.PositionDto;
import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.TileType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Client-side prediction for the local player. A step is shown as soon as it is sent, using the
 * entry rules of the server's {@code BombermanGame.movePlayer} (empty tile, no bomb, no living
 * player), and every snapshot reconciles it: start from the authoritative position, drop the steps
 * the server acknowledged ({@code lastMoveSeq}) and replay the rest. A wrong guess therefore only
 * lasts until the next snapshot. Used on the UI thread only.
 */
public final class LocalPrediction {

    private record PendingMove(long seq, Direction direction) {
    }

    private final Deque<PendingMove> pending = new ArrayDeque<>();
    /** Never reset, so a late acknowledgement from an old match cannot match a new move. */
    private long nextSeq = 1;
    private long userId;
    private GameStateDto authoritative;
    private PositionDto position;
    private boolean alive;

    public void reset() {
        pending.clear();
        authoritative = null;
        position = null;
        alive = false;
    }

    /** Applies the newest snapshot (which must carry a map) and replays unacknowledged steps. */
    public void accept(GameStateDto snapshot, long userId) {
        this.authoritative = Objects.requireNonNull(snapshot, "snapshot must not be null");
        this.userId = userId;
        GamePlayerStateDto me = snapshot.players().stream()
                .filter(player -> player.userId() == userId)
                .findFirst()
                .orElse(null);
        if (me == null || !me.alive()) {
            pending.clear();
            position = me == null ? null : me.position();
            alive = false;
            return;
        }
        alive = true;
        while (!pending.isEmpty() && pending.peekFirst().seq() <= me.lastMoveSeq()) {
            pending.removeFirst();
        }
        PositionDto replayed = me.position();
        for (PendingMove move : pending) {
            replayed = step(replayed, move.direction());
        }
        position = replayed;
    }

    /** Records a move that is about to be sent, moves the prediction, and returns the move's seq. */
    public long predictMove(Direction direction) {
        Objects.requireNonNull(direction, "direction must not be null");
        long seq = nextSeq++;
        if (alive && position != null) {
            pending.addLast(new PendingMove(seq, direction));
            position = step(position, direction);
        }
        return seq;
    }

    /** Where the local player should be drawn; null when the local player is not in the match. */
    public PositionDto position() {
        return position;
    }

    /** Sent moves the server has not acknowledged yet. */
    public int pendingCount() {
        return pending.size();
    }

    private PositionDto step(PositionDto from, Direction direction) {
        PositionDto to = switch (direction) {
            case UP -> new PositionDto(from.column(), from.row() - 1);
            case DOWN -> new PositionDto(from.column(), from.row() + 1);
            case LEFT -> new PositionDto(from.column() - 1, from.row());
            case RIGHT -> new PositionDto(from.column() + 1, from.row());
        };
        return canEnter(to) ? to : from;
    }

    private boolean canEnter(PositionDto to) {
        List<List<TileType>> map = authoritative.map();
        if (map == null || to.row() < 0 || to.row() >= map.size()) {
            return false;
        }
        List<TileType> row = map.get(to.row());
        if (to.column() < 0 || to.column() >= row.size() || row.get(to.column()) != TileType.EMPTY) {
            return false;
        }
        if (authoritative.bombs().stream().anyMatch(bomb -> bomb.position().equals(to))) {
            return false;
        }
        return authoritative.players().stream()
                .noneMatch(other -> other.userId() != userId && other.alive() && other.position().equals(to));
    }
}
