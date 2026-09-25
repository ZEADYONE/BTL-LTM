package com.bomberman.server.room;

import com.bomberman.common.dto.RoomPlayerDto;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.dto.RoomSummaryDto;
import org.springframework.stereotype.Component;

@Component
public class RoomDtoMapper {

    public RoomStateDto toState(RoomSnapshot room) {
        return new RoomStateDto(
                room.roomId(),
                room.roomName(),
                room.hostUserId(),
                room.players().stream()
                        .map(player -> new RoomPlayerDto(
                                player.userId(),
                                player.username(),
                                player.ready()
                        ))
                        .toList(),
                room.maxPlayers(),
                room.status()
        );
    }

    public RoomSummaryDto toSummary(RoomSnapshot room) {
        return new RoomSummaryDto(
                room.roomId(),
                room.roomName(),
                room.hostUserId(),
                room.players().size(),
                room.maxPlayers(),
                room.status()
        );
    }
}
