package com.bomberman.server.auth;

import com.bomberman.common.enums.AuthResultCode;

record LoginResult(AuthResultCode result, AuthenticatedUser authenticatedUser) {

    boolean isSuccess() {
        return result == AuthResultCode.SUCCESS;
    }
}
