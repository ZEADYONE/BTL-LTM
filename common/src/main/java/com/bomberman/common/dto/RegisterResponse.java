package com.bomberman.common.dto;

import com.bomberman.common.enums.AuthResultCode;

/** Result of an account registration attempt. */
public record RegisterResponse(
        boolean success,
        AuthResultCode result,
        Long userId,
        String username
) {
}
