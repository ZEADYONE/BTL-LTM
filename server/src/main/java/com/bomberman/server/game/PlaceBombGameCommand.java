package com.bomberman.server.game;

import java.time.Instant;

public record PlaceBombGameCommand(long userId) implements GameCommand {

    @Override
    public void execute(BombermanGame game, Instant tickTime) {
        game.placeBomb(userId, tickTime);
    }
}
