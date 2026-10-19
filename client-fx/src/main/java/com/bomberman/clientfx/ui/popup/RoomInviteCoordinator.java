package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.common.dto.RoomInviteDto;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

/** Serialises incoming invitations so they never replace another popup already on screen. */
public final class RoomInviteCoordinator implements ClientStateListener, AutoCloseable {

    private final ClientState state;
    private final GameClientController controller;
    private final AppShell shell;
    private final Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), event -> refresh()));
    private final Runnable modalClosed = this::refresh;
    private String activeInviteId;
    private RoomInvitePopup activePopup;

    public RoomInviteCoordinator(ClientState state, GameClientController controller, AppShell shell) {
        this.state = state;
        this.controller = controller;
        this.shell = shell;
        countdown.setCycleCount(Timeline.INDEFINITE);
        state.addListener(this);
        shell.addModalClosedListener(modalClosed);
    }

    @Override
    public void onClientStateChanged() {
        refresh();
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        state.getPendingRoomInvites().stream()
                .filter(invite -> invite.expiresAtEpochMillis() <= now)
                .map(RoomInviteDto::inviteId)
                .toList()
                .forEach(state::removeRoomInvite);

        RoomInviteDto active = state.getPendingRoomInvites().stream()
                .filter(invite -> invite.inviteId().equals(activeInviteId))
                .findFirst()
                .orElse(null);
        if (activeInviteId != null && active == null) {
            activeInviteId = null;
            activePopup = null;
            countdown.stop();
            if (shell.isModalShowing()) {
                shell.closeModal();
            }
        }
        if (active != null) {
            updateCountdown(active, now);
            return;
        }
        if (shell.isModalShowing()) {
            return;
        }
        state.getPendingRoomInvites().stream().findFirst().ifPresent(this::show);
    }

    private void show(RoomInviteDto invite) {
        activeInviteId = invite.inviteId();
        activePopup = new RoomInvitePopup(
                invite,
                () -> respond(invite, false),
                () -> respond(invite, true)
        );
        updateCountdown(invite, System.currentTimeMillis());
        shell.showModal(activePopup.root(), false);
        countdown.playFromStart();
    }

    private void updateCountdown(RoomInviteDto invite, long now) {
        long millis = Math.max(0, invite.expiresAtEpochMillis() - now);
        activePopup.setRemainingSeconds((millis + 999) / 1000);
    }

    private void respond(RoomInviteDto invite, boolean accept) {
        activePopup.setLoading(true);
        controller.respondRoomInvite(invite.inviteId(), accept).whenComplete((reply, failure) -> {
            // A response consumes the invitation even when room conditions changed meanwhile.
            state.removeRoomInvite(invite.inviteId());
        });
    }

    @Override
    public void close() {
        countdown.stop();
        state.removeListener(this);
        shell.removeModalClosedListener(modalClosed);
    }
}
