package com.bomberman.common.dto;

import com.bomberman.common.enums.PlayerStatus;

public record OnlineUserDto(long userId, String username, PlayerStatus status) {
}
