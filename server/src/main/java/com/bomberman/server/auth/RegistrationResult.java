package com.bomberman.server.auth;

import com.bomberman.common.enums.AuthResultCode;

record RegistrationResult(
        AuthResultCode result,
        Long userId,
        String username
) {

    boolean isSuccess() {
        return result == AuthResultCode.SUCCESS;
    }
}
