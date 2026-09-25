package com.bomberman.common.dto;

import java.util.List;

public record RoomListUpdate(List<RoomSummaryDto> rooms) {

    public RoomListUpdate {
        rooms = List.copyOf(rooms);
    }
}
