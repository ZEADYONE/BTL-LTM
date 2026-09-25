package com.bomberman.common.dto;

/** Credentials submitted when starting an authenticated session. */
public record LoginRequest(String username, String password) {
}
