package com.bomberman.common.dto;

import com.bomberman.common.enums.AuthResultCode;
import com.bomberman.common.enums.PlayerStatus;

/** Result of a login attempt. */
public record LoginResponse(
        boolean success,
        AuthResultCode result,
        Long userId,
        String username,
        PlayerStatus status
) {
}
