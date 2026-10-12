package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.common.dto.FriendDto;
import com.bomberman.common.dto.RoomStateDto;
import com.bomberman.common.enums.FriendPresenceStatus;
import com.bomberman.common.enums.MessageType;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Set;

/** Friend picker shown from a waiting room. */
public final class InviteFriendsPopup {

    private final ClientState state;
    private final GameClientController controller;
    private final AppShell shell;
    private final Set<Long> sent = new HashSet<>();
    private final VBox rows = new VBox(8);

    public InviteFriendsPopup(ClientState state, GameClientController controller, AppShell shell) {
        this.state = state;
        this.controller = controller;
        this.shell = shell;
    }

    public void show() {
        rebuild();
        ScrollPane scroll = new ScrollPane(rows);
        scroll.getStyleClass().add("game-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(320);
        GameButton close = new GameButton("CLOSE", GameButton.Tone.BLUE, GameButton.Size.M);
        close.setOnAction(event -> shell.closeModal());
        shell.showModal(Popups.frame("INVITE FRIENDS", 560, scroll, close), true);
    }

    private void rebuild() {
        rows.getChildren().clear();
        RoomStateDto room = state.getRoom();
        if (room == null) {
            rows.getChildren().add(Popups.text("You are no longer in a room."));
            return;
        }
        Set<Long> members = new HashSet<>();
        room.players().forEach(player -> members.add(player.userId()));
        var candidates = state.getFriends().stream()
                .filter(friend -> !members.contains(friend.userId()))
                .toList();
        if (candidates.isEmpty()) {
            rows.getChildren().add(Popups.text("No friends are available to invite."));
            return;
        }
        candidates.forEach(friend -> rows.getChildren().add(row(friend)));
    }

    private Node row(FriendDto friend) {
        Label name = new Label(friend.username());
        name.getStyleClass().add("friend-name");
        Label presence = new Label(presence(friend.presence()));
        presence.getStyleClass().add("friend-presence");
        VBox identity = new VBox(2, name, presence);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        GameButton invite = new GameButton(sent.contains(friend.userId()) ? "SENT" : "INVITE",
                GameButton.Tone.GREEN, GameButton.Size.S);
        invite.setPrefWidth(130);
        invite.setDisable(friend.presence() != FriendPresenceStatus.FREE || sent.contains(friend.userId()));
        invite.setOnAction(event -> {
            invite.setLoading(true);
            controller.sendRoomInvite(friend.userId()).whenComplete((reply, failure) -> {
                invite.setLoading(false);
                if (reply != null && reply.type() == MessageType.ROOM_INVITE_STATUS) {
                    sent.add(friend.userId());
                    invite.setLabelText("SENT");
                    invite.setDisable(true);
                    state.setFeedback(Feedback.success("Invitation sent to " + friend.username() + "."));
                }
            });
        });
        HBox row = new HBox(12, identity, spacer, invite);
        row.getStyleClass().add("friend-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private static String presence(FriendPresenceStatus presence) {
        return switch (presence) {
            case FREE -> "IN LOBBY";
            case OFFLINE -> "OFFLINE";
            case IN_ROOM -> "IN ROOM";
            case PLAYING -> "PLAYING";
        };
    }
}
