package com.bomberman.common.enums;

/**
 * Machine-readable result of a register or login request.
 */
public enum AuthResultCode {
    SUCCESS,
    INVALID_REQUEST,
    INVALID_USERNAME,
    INVALID_PASSWORD,
    USERNAME_ALREADY_EXISTS,
    INVALID_CREDENTIALS,
    ACCOUNT_ALREADY_ONLINE,
    SESSION_ALREADY_AUTHENTICATED
}
