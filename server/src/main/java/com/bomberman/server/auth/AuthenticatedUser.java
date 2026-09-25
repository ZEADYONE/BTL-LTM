package com.bomberman.server.auth;

/**
 * Lightweight authenticated identity attached to a TCP session.
 */
public record AuthenticatedUser(Long userId, String username) {
}
