package com.bomberman.server.game;

import java.time.Instant;

/** Input queued by a network thread and executed only by the room game-loop thread. */
public sealed interface GameCommand permits MoveGameCommand, PlaceBombGameCommand,
        DisconnectGameCommand {

    long userId();

    void execute(BombermanGame game, Instant tickTime);
}
