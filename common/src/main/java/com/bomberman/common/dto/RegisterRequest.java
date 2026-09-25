package com.bomberman.common.dto;

/** Credentials submitted when creating an account. */
public record RegisterRequest(String username, String password) {
}
