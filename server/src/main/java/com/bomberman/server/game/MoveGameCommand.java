package com.bomberman.server.game;

import com.bomberman.common.enums.Direction;

import java.time.Instant;
import java.util.Objects;

public record MoveGameCommand(long userId, Direction direction) implements GameCommand {

    public MoveGameCommand {
        Objects.requireNonNull(direction, "direction must not be null");
    }

    @Override
    public void execute(BombermanGame game, Instant tickTime) {
        game.movePlayer(userId, direction);
    }
}
