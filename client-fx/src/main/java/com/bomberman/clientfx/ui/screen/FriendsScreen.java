package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.InputValidation;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.GameFields;
import com.bomberman.clientfx.ui.component.GameTabs;
import com.bomberman.clientfx.ui.component.PlayerHead;
import com.bomberman.clientfx.ui.component.SvgView;
import com.bomberman.clientfx.ui.component.TitleBanner;
import com.bomberman.clientfx.ui.popup.Popups;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.dto.FriendDto;
import com.bomberman.common.dto.FriendRequestDto;
import com.bomberman.common.enums.FriendPresenceStatus;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;
import java.util.HashSet;
import java.util.Set;

/** Friend list and incoming/outgoing request management. */
public final class FriendsScreen implements Screen, ClientStateListener {

    private static final int FRIENDS_TAB = 0;
    private static final int REQUESTS_TAB = 1;

    private final ClientState state;
    private final GameClientController controller;
    private final SvgAssets assets;
    private final AppShell shell;
    private final AnchorPane root = new AnchorPane();
    private final TextField username = GameFields.text("Exact username");
    private final Label usernameError = new Label();
    private final GameButton add = new GameButton("ADD FRIEND", GameButton.Tone.YELLOW, GameButton.Size.M);
    private final GameTabs tabs = new GameTabs("FRIENDS", "REQUESTS");
    private final VBox content = new VBox(10);
    private final Label summary = new Label();
    private final Set<Long> invitedUserIds = new HashSet<>();

    public FriendsScreen(
            ClientState state,
            GameClientController controller,
            Navigator navigator,
            SvgAssets assets,
            AppShell shell
    ) {
        this.state = state;
        this.controller = controller;
        this.assets = assets;
        this.shell = shell;

        GameButton back = new GameButton(
                "BACK",
                GameButton.Tone.BLUE,
                GameButton.Size.M,
                icon("mdi2a-arrow-left-bold", 24)
        );
        back.setOnAction(event -> navigator.show(ScreenId.HOME));
        AnchorPane.setLeftAnchor(back, 24.0);
        AnchorPane.setTopAnchor(back, 24.0);

        TitleBanner title = TitleBanner.purple(
                "FRIENDS",
                new SvgView(assets, AssetIds.ICON_PLAYERS, 52, 52)
        );
        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);
        AnchorPane.setLeftAnchor(titleRow, 0.0);
        AnchorPane.setRightAnchor(titleRow, 0.0);
        AnchorPane.setTopAnchor(titleRow, 18.0);

        username.setPrefWidth(470);
        username.setOnAction(event -> addFriend());
        username.textProperty().addListener(observable -> {
            usernameError.setText("");
            GameFields.markError(username, false);
        });
        add.setPrefWidth(210);
        add.setOnAction(event -> addFriend());
        HBox addRow = new HBox(12, username, add);
        addRow.setAlignment(Pos.CENTER);
        usernameError.getStyleClass().add("field-error");
        usernameError.managedProperty().bind(usernameError.visibleProperty());
        VBox addArea = new VBox(4, addRow, usernameError);
        addArea.setAlignment(Pos.TOP_CENTER);
        addArea.setPrefWidth(692);
        AnchorPane.setLeftAnchor(addArea, 230.0);
        AnchorPane.setTopAnchor(addArea, 102.0);

        tabs.selectedIndexProperty().addListener(observable -> rebuild());
        AnchorPane.setLeftAnchor(tabs, 40.0);
        AnchorPane.setTopAnchor(tabs, 184.0);

        summary.getStyleClass().add("friends-summary");
        AnchorPane.setRightAnchor(summary, 40.0);
        AnchorPane.setTopAnchor(summary, 198.0);

        content.getStyleClass().add("friends-list");
        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("game-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        StackPane listArea = new StackPane(scroll);
        listArea.getStyleClass().add("friends-list-area");
        AnchorPane.setLeftAnchor(listArea, 40.0);
        AnchorPane.setRightAnchor(listArea, 40.0);
        AnchorPane.setTopAnchor(listArea, 244.0);
        AnchorPane.setBottomAnchor(listArea, 22.0);

        root.getChildren().addAll(listArea, tabs, summary, addArea, titleRow, back);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        invitedUserIds.clear();
        add.setLoading(false);
        usernameError.setVisible(false);
        onClientStateChanged();
        controller.requestFriends();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
    }

    @Override
    public void onClientStateChanged() {
        int online = (int) state.getFriends().stream()
                .filter(friend -> friend.presence() != FriendPresenceStatus.OFFLINE)
                .count();
        summary.setText(online + " ONLINE  ·  " + state.getFriends().size() + " FRIENDS");
        tabs.setLabel(
                REQUESTS_TAB,
                state.getIncomingFriendRequests().isEmpty()
                        ? "REQUESTS"
                        : "REQUESTS (" + state.getIncomingFriendRequests().size() + ")"
        );
        rebuild();
    }

    private void rebuild() {
        if (tabs.selectedIndexProperty().get() == FRIENDS_TAB) {
            rebuildFriends();
        } else {
            rebuildRequests();
        }
    }

    private void rebuildFriends() {
        content.getChildren().clear();
        if (state.getFriends().isEmpty()) {
            content.getChildren().add(empty("No friends yet. Add someone by username!"));
            return;
        }
        state.getFriends().forEach(friend -> content.getChildren().add(friendRow(friend)));
    }

    private void rebuildRequests() {
        content.getChildren().clear();
        if (state.getIncomingFriendRequests().isEmpty()
                && state.getOutgoingFriendRequests().isEmpty()) {
            content.getChildren().add(empty("No pending friend requests."));
            return;
        }
        if (!state.getIncomingFriendRequests().isEmpty()) {
            content.getChildren().add(section("INCOMING"));
            state.getIncomingFriendRequests().forEach(
                    request -> content.getChildren().add(incomingRow(request))
            );
        }
        if (!state.getOutgoingFriendRequests().isEmpty()) {
            Label outgoing = section("OUTGOING");
            VBox.setMargin(outgoing, new Insets(10, 0, 0, 0));
            content.getChildren().add(outgoing);
            state.getOutgoingFriendRequests().forEach(
                    request -> content.getChildren().add(outgoingRow(request))
            );
        }
    }

    private Node friendRow(FriendDto friend) {
        HBox row = baseRow(friend.userId(), friend.username(), friend.presence());
        if (canInvite(friend)) {
            GameButton invite = action(
                    invitedUserIds.contains(friend.userId()) ? "SENT" : "INVITE",
                    GameButton.Tone.GREEN
            );
            invite.setDisable(invitedUserIds.contains(friend.userId()));
            invite.setOnAction(event -> {
                invite.setLoading(true);
                controller.sendRoomInvite(friend.userId()).whenComplete((reply, failure) -> {
                    invite.setLoading(false);
                    if (reply != null && reply.type() == MessageType.ROOM_INVITE_STATUS) {
                        invitedUserIds.add(friend.userId());
                        invite.setLabelText("SENT");
                        invite.setDisable(true);
                        state.setFeedback(Feedback.success("Invitation sent to " + friend.username() + "."));
                    }
                });
            });
            row.getChildren().add(invite);
        }
        GameButton remove = action("REMOVE", GameButton.Tone.RED);
        remove.setTooltip(new Tooltip("Remove " + friend.username() + " from your friends"));
        remove.setOnAction(event -> Popups.confirm(
                shell,
                "REMOVE FRIEND",
                "Remove " + friend.username() + " from your friends?",
                "REMOVE",
                () -> {
                    remove.setLoading(true);
                    controller.removeFriend(friend.userId()).whenComplete((reply, failure) -> {
                        remove.setLoading(false);
                        announceSuccess(reply, "Friend removed.");
                    });
                }
        ));
        row.getChildren().add(remove);
        return row;
    }

    private boolean canInvite(FriendDto friend) {
        var room = state.getRoom();
        return room != null
                && room.status() == RoomStatus.WAITING
                && friend.presence() == FriendPresenceStatus.FREE
                && room.players().stream().noneMatch(player -> player.userId() == friend.userId());
    }

    private Node incomingRow(FriendRequestDto request) {
        HBox row = baseRow(request.userId(), request.username(), request.presence());
        GameButton decline = action("DECLINE", GameButton.Tone.RED);
        GameButton accept = action("ACCEPT", GameButton.Tone.GREEN);
        decline.setOnAction(event -> respond(request, false, decline, accept));
        accept.setOnAction(event -> respond(request, true, accept, decline));
        row.getChildren().addAll(decline, accept);
        return row;
    }

    private Node outgoingRow(FriendRequestDto request) {
        HBox row = baseRow(request.userId(), request.username(), request.presence());
        GameButton cancel = action("CANCEL", GameButton.Tone.GREY);
        cancel.setOnAction(event -> {
            cancel.setLoading(true);
            controller.cancelFriendRequest(request.relationshipId()).whenComplete((reply, failure) -> {
                cancel.setLoading(false);
                announceSuccess(reply, "Friend request cancelled.");
            });
        });
        row.getChildren().add(cancel);
        return row;
    }

    private HBox baseRow(long userId, String playerName, FriendPresenceStatus presence) {
        PlayerHead avatar = new PlayerHead(assets, TeamColor.forUser(userId), 54, true);
        Label name = new Label(playerName);
        name.getStyleClass().add("friend-name");
        Label status = new Label(presenceText(presence));
        status.getStyleClass().addAll(
                "friend-presence",
                "presence-" + presence.name().toLowerCase(java.util.Locale.ROOT)
        );
        VBox identity = new VBox(2, name, status);
        identity.setAlignment(Pos.CENTER_LEFT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(12, avatar, identity, spacer);
        row.getStyleClass().add("friend-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(Double.MAX_VALUE);
        return row;
    }

    private void addFriend() {
        String normalized = username.getText() == null ? "" : username.getText().strip();
        Optional<String> problem = InputValidation.usernameError(normalized);
        if (problem.isEmpty() && normalized.equals(state.getCurrentUsername())) {
            problem = Optional.of("You cannot add yourself.");
        }
        GameFields.markError(username, problem.isPresent());
        usernameError.setText(problem.orElse(""));
        usernameError.setVisible(problem.isPresent());
        if (problem.isPresent() || add.isLoading()) {
            return;
        }

        add.setLoading(true);
        controller.sendFriendRequest(normalized).whenComplete((reply, failure) -> {
            add.setLoading(false);
            if (reply != null && reply.type() == MessageType.FRIEND_LIST_UPDATE) {
                username.clear();
                state.setFeedback(Feedback.success("Friend request sent."));
            }
        });
    }

    private void respond(
            FriendRequestDto request,
            boolean accept,
            GameButton primary,
            GameButton other
    ) {
        primary.setLoading(true);
        other.setDisable(true);
        controller.respondFriendRequest(request.relationshipId(), accept)
                .whenComplete((reply, failure) -> {
                    primary.setLoading(false);
                    other.setDisable(false);
                    announceSuccess(reply, accept ? "Friend added." : "Friend request declined.");
                });
    }

    private void announceSuccess(com.bomberman.common.message.NetworkMessage reply, String message) {
        if (reply != null && reply.type() == MessageType.FRIEND_LIST_UPDATE) {
            state.setFeedback(Feedback.success(message));
        }
    }

    private static GameButton action(String text, GameButton.Tone tone) {
        GameButton button = new GameButton(text, tone, GameButton.Size.S);
        button.setPrefWidth(130);
        return button;
    }

    private static Label section(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("friend-section");
        return label;
    }

    private static Label empty(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER);
        VBox.setMargin(label, new Insets(90, 0, 0, 0));
        return label;
    }

    private static String presenceText(FriendPresenceStatus presence) {
        return switch (presence) {
            case OFFLINE -> "OFFLINE";
            case FREE -> "IN LOBBY";
            case IN_ROOM -> "IN ROOM";
            case PLAYING -> "PLAYING";
        };
    }

    private static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }
}
