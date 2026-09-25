package com.bomberman.server.game;

public interface GameLoopListener {

    void onGameState(String roomId, BombermanSnapshot snapshot);

    void onGameOver(String roomId, BombermanGame game, GameOutcome outcome);
}
