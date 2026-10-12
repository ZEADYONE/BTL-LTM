package com.bomberman.common.enums;

/** Client-visible lifecycle of an ephemeral invitation to a room. */
public enum RoomInviteStatus {
    SENT,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED
}
