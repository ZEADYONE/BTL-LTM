package com.bomberman.server.game;

import com.bomberman.common.enums.Direction;

import java.util.Objects;

/** Zero-based grid coordinate, with row zero at the top of the map. */
public record Position(int column, int row) {

    public Position move(Direction direction) {
        return switch (Objects.requireNonNull(direction, "direction must not be null")) {
            case UP -> new Position(column, row - 1);
            case DOWN -> new Position(column, row + 1);
            case LEFT -> new Position(column - 1, row);
            case RIGHT -> new Position(column + 1, row);
        };
    }
}
