package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.common.dto.RoomSummaryDto;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * S2, temporary layout for phase 1: proves that login, presence and room list updates
 * arrive over the new client. The styled Home is built in phase 3.
 */
public final class HomeScreen implements Screen, ClientStateListener {

    private final ClientState state;
    private final GameClientController controller;
    private final StackPane root = new StackPane();
    private final Label player = new Label();
    private final Label online = new Label();
    private final ListView<String> rooms = new ListView<>();

    public HomeScreen(ClientState state, GameClientController controller, Navigator navigator) {
        this.state = state;
        this.controller = controller;

        Label title = new Label("HOME");
        title.getStyleClass().add("temp-title");
        Label note = new Label("Temporary screen – the final Home arrives in phase 3.");
        note.getStyleClass().add("temp-note");
        rooms.setPrefHeight(220);
        rooms.setPlaceholder(new Label("No rooms yet"));

        Button logout = new Button("LOG OUT");
        logout.setOnAction(event -> {
            controller.logout();
            navigator.show(ScreenId.LOGIN);
        });

        VBox panel = new VBox(12, title, note, player, online, new Label("Rooms"), rooms, logout);
        panel.getStyleClass().add("temp-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setMaxSize(560, VBox.USE_PREF_SIZE);
        root.getChildren().add(panel);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        onClientStateChanged();
        controller.requestLobbyState();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
    }

    @Override
    public void onClientStateChanged() {
        player.setText("Logged in as " + (state.isLoggedIn() ? state.getCurrentUsername() : "-"));
        online.setText("Online players: " + state.getOnlineUsers().size());
        rooms.getItems().setAll(state.getRooms().stream().map(HomeScreen::describe).toList());
    }

    private static String describe(RoomSummaryDto room) {
        return room.roomName() + "   " + room.playerCount() + "/" + room.maxPlayers() + "   " + room.status();
    }
}
