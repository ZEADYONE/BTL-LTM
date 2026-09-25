package com.bomberman.common.dto;

/**
 * Room state for a member, or a leave acknowledgement when member is false.
 */
public record RoomStateUpdate(boolean member, RoomStateDto room) {
}
