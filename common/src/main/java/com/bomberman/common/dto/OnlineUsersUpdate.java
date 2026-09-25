package com.bomberman.common.dto;

import java.util.List;

public record OnlineUsersUpdate(List<OnlineUserDto> users) {

    public OnlineUsersUpdate {
        users = List.copyOf(users);
    }
}
