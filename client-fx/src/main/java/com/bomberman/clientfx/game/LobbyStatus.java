package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.common.dto.RoomPlayerDto;
import com.bomberman.common.dto.RoomStateDto;

/**
 * What the Room Lobby shows about starting (ROOM STATUS checklist, READY and START buttons).
 * Mirrors RoomManager.startGame: host only, at least two players, everyone ready (host included).
 */
public record LobbyStatus(int players, int maxPlayers, int readyPlayers, boolean host, boolean youReady) {

    public static LobbyStatus of(RoomStateDto room, long userId) {
        int ready = (int) room.players().stream().filter(RoomPlayerDto::ready).count();
        boolean youReady = room.players().stream()
                .anyMatch(player -> player.userId() == userId && player.ready());
        return new LobbyStatus(room.players().size(), room.maxPlayers(), ready, room.hostUserId() == userId, youReady);
    }

    public boolean enoughPlayers() {
        return players >= ServerRules.MIN_PLAYERS;
    }

    public boolean everyoneReady() {
        return players > 0 && readyPlayers == players;
    }

    public boolean canStart() {
        return host && enoughPlayers() && everyoneReady();
    }

    /** Last line of the ROOM STATUS panel. */
    public String statusLine() {
        if (!enoughPlayers() || !everyoneReady()) {
            return "Waiting for players…";
        }
        return host ? "Ready to start!" : "Waiting for host to start…";
    }
}
