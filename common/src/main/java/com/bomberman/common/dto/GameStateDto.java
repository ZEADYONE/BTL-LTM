package com.bomberman.common.dto;

import com.bomberman.common.enums.GameStatus;
import com.bomberman.common.enums.TileType;

import java.util.List;

/**
 * Authoritative snapshot sent to every player in a room. {@code map} is null when the server
 * left it out because it has not changed; receivers keep the last map with {@link #withMap}.
 */
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
        map = map == null ? null : map.stream().map(List::copyOf).toList();
        players = List.copyOf(players);
        bombs = List.copyOf(bombs);
        explosions = List.copyOf(explosions);
    }

    public GameStateDto withMap(List<List<TileType>> map) {
        return new GameStateDto(tick, gameStatus, map, players, bombs, explosions, remainingPlayers);
    }
}
