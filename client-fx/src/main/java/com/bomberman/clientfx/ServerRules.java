package com.bomberman.clientfx;

/**
 * Server-side rules the client relies on for validation and presentation.
 * Keep in sync with the server; sources are listed in docs/ui-redesign/03-kien-truc-ky-thuat.md, section 9.
 */
public final class ServerRules {

    /** AuthenticationService.MAX_USERNAME_LENGTH. */
    public static final int MAX_USERNAME_LENGTH = 50;

    /** RoomManager.MAX_ROOM_NAME_LENGTH (after trimming). */
    public static final int MAX_ROOM_NAME_LENGTH = 60;

    private ServerRules() {
    }
}
