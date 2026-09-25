package com.bomberman.server.game;

import com.bomberman.common.dto.BombStateDto;
import com.bomberman.common.dto.ExplosionStateDto;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.PositionDto;
import com.bomberman.common.enums.TileType;
import org.springframework.stereotype.Component;

@Component
public class GameStateMapper {

    public GameStateDto toDto(BombermanSnapshot snapshot) {
        return new GameStateDto(
                snapshot.tick(),
                snapshot.status(),
                snapshot.map().stream()
                        .map(row -> row.stream().map(this::toTileType).toList())
                        .toList(),
                snapshot.players().stream()
                        .map(player -> new GamePlayerStateDto(
                                player.userId(),
                                player.username(),
                                toPosition(player.position()),
                                player.alive(),
                                player.bombCapacity(),
                                player.activeBombs(),
                                player.bombRange()
                        ))
                        .toList(),
                snapshot.bombs().stream()
                        .map(bomb -> new BombStateDto(
                                bomb.bombId(),
                                bomb.ownerUserId(),
                                toPosition(bomb.position()),
                                bomb.blastRange(),
                                bomb.remainingFuseMillis()
                        ))
                        .toList(),
                snapshot.explosions().stream()
                        .map(explosion -> new ExplosionStateDto(
                                toPosition(explosion.origin()),
                                explosion.affectedPositions().stream()
                                        .map(this::toPosition)
                                        .toList(),
                                explosion.remainingMillis()
                        ))
                        .toList(),
                snapshot.remainingPlayers()
        );
    }

    private PositionDto toPosition(Position position) {
        return new PositionDto(position.column(), position.row());
    }

    private TileType toTileType(Tile tile) {
        return TileType.valueOf(tile.name());
    }
}
