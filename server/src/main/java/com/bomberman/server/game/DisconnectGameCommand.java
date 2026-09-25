package com.bomberman.server.game;

import java.time.Instant;

/** A transport disconnect translated into an authoritative game-loop command. */
public record DisconnectGameCommand(long userId) implements GameCommand {

    @Override
    public void execute(BombermanGame game, Instant tickTime) {
        game.disconnectPlayer(userId);
    }
}
