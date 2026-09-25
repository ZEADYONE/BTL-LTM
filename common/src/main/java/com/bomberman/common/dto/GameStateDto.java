package com.bomberman.common.dto;

import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.TileType;

import java.util.List;

/** Full authoritative snapshot sent to every player in a room. */
public record GameStateDto(
        long tick,
        GameStatus gameStatus,
        List<List<TileType>> map,
        List<GamePlayerStateDto> players,
        List<BombStateDto> bombs,
        List<ExplosionStateDto> explosions,
        int remainingPlayers
) {

    public GameStateDto {
        map = map.stream().map(List::copyOf).toList();
        players = List.copyOf(players);
        bombs = List.copyOf(bombs);
        explosions = List.copyOf(explosions);
    }
}
