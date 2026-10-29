package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.Panel;
import com.bomberman.common.dto.RoomInviteDto;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Non-dismissible incoming room invitation with an explicit accept/decline decision. */
public final class RoomInvitePopup {

    private final GameButton decline = new GameButton("DECLINE", GameButton.Tone.RED, GameButton.Size.M);
    private final GameButton accept = new GameButton("JOIN ROOM", GameButton.Tone.GREEN, GameButton.Size.M);
    private final Label countdown = new Label();
    private final Panel root;

    public RoomInvitePopup(RoomInviteDto invite, Runnable onDecline, Runnable onAccept) {
        Label message = Popups.text(invite.inviterUsername() + " invited you to join");
        Label room = new Label(invite.roomName());
        room.getStyleClass().add("room-invite-name");
        countdown.getStyleClass().add("room-invite-countdown");
        VBox body = new VBox(8, message, room, countdown);
        body.setAlignment(Pos.CENTER);
        decline.setOnAction(event -> onDecline.run());
        accept.setOnAction(event -> onAccept.run());
        root = Popups.frame("ROOM INVITATION", 500, body, decline, accept);
    }

    public Node root() {
        return root;
    }

    public void setRemainingSeconds(long seconds) {
        countdown.setText("Expires in " + Math.max(0, seconds) + "s");
    }

    public void setLoading(boolean loading) {
        accept.setLoading(loading);
        decline.setDisable(loading);
    }
}
