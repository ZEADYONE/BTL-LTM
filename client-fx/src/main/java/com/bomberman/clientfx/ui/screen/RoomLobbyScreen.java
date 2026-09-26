package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.game.LobbyStatus;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.component.ArenaPreview;
import com.bomberman.clientfx.ui.component.ChecklistItem;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.Panel;
import com.bomberman.clientfx.ui.component.SlotCard;
import com.bomberman.clientfx.ui.component.SvgView;
import com.bomberman.clientfx.ui.component.TitleBanner;
import com.bomberman.clientfx.ui.popup.Popups;
import com.bomberman.common.dto.RoomPlayerDto;
import com.bomberman.common.dto.RoomStateDto;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

/** S4 Room Lobby (mockup 4, elements R-01…R-20 in docs/ui-redesign/01). */
public final class RoomLobbyScreen implements Screen, ClientStateListener {

    private static final double PREVIEW_TILE = 16;

    private final ClientState state;
    private final GameClientController controller;
    private final AnchorPane root = new AnchorPane();
    private final Label roomName = new Label();
    private final List<SlotCard> slots = new ArrayList<>();
    private final ArenaPreview arena;
    private final Label playersValue = new Label();
    private final ChecklistItem playersCheck = new ChecklistItem(ChecklistItem.State.NOT_DONE, "");
    private final ChecklistItem readyCheck = new ChecklistItem(ChecklistItem.State.NOT_DONE, "");
    private final ChecklistItem statusLine = new ChecklistItem(ChecklistItem.State.WAITING, "");
    private final GameButton ready = new GameButton("READY", GameButton.Tone.GREEN, GameButton.Size.L,
            icon("mdi2c-check-bold", 30));
    private final GameButton start = new GameButton("START GAME", GameButton.Tone.YELLOW, GameButton.Size.L);
    private LobbyStatus status;

    public RoomLobbyScreen(ClientState state, GameClientController controller, SvgAssets assets, AppShell shell) {
        this.state = state;
        this.controller = controller;

        TitleBanner title = TitleBanner.purple("ROOM LOBBY", new SvgView(assets, AssetIds.ICON_PLAYERS, 52, 52));
        AnchorPane.setLeftAnchor(title, 24.0);
        AnchorPane.setTopAnchor(title, 18.0);

        GameButton leave = new GameButton("", GameButton.Tone.RED, GameButton.Size.ICON, icon("mdi2c-close-thick", 30));
        leave.setOnAction(event -> Popups.confirm(shell, "LEAVE ROOM", "Leave this room?", "LEAVE",
                controller::leaveRoom));
        AnchorPane.setRightAnchor(leave, 24.0);
        AnchorPane.setTopAnchor(leave, 24.0);

        Label nameLabel = new Label("ROOM NAME");
        nameLabel.getStyleClass().add("info-label");
        roomName.getStyleClass().add("room-name-value");
        HBox nameBox = new HBox(14, nameLabel, roomName);
        nameBox.getStyleClass().add("room-name-box");
        HBox.setHgrow(roomName, Priority.ALWAYS);
        nameBox.setPrefWidth(588);
        AnchorPane.setLeftAnchor(nameBox, 24.0);
        AnchorPane.setTopAnchor(nameBox, 104.0);

        GridPane slotGrid = new GridPane();
        slotGrid.setHgap(14);
        slotGrid.setVgap(14);
        for (int slot = 0; slot < ServerRules.MAX_PLAYERS; slot++) {
            SlotCard card = new SlotCard(assets, slot);
            slots.add(card);
            slotGrid.add(card, slot % 2, slot / 2);
        }
        AnchorPane.setLeftAnchor(slotGrid, 24.0);
        AnchorPane.setTopAnchor(slotGrid, 170.0);

        arena = new ArenaPreview(assets, PREVIEW_TILE);
        Panel mapPanel = new Panel(Panel.Style.PURPLE, "MAP · CLASSIC ARENA", arena);
        mapPanel.body().setAlignment(Pos.CENTER);

        VBox info = new VBox(12,
                infoRow(assets, AssetIds.BOMB, "GAME MODE", new Label("CLASSIC")),
                infoRow(assets, AssetIds.ICON_TROPHY, "WIN", new Label("LAST ONE STANDING")),
                infoRow(assets, AssetIds.ICON_PLAYERS, "PLAYERS", playersValue));
        Panel infoPanel = new Panel(Panel.Style.BROWN, "INFO", info);
        infoPanel.setPrefWidth(196);
        HBox top = new HBox(14, mapPanel, infoPanel);
        Panel statusPanel = new Panel(Panel.Style.PURPLE, "ROOM STATUS", playersCheck, readyCheck, statusLine);
        statusPanel.body().setSpacing(6);
        statusPanel.body().setStyle("-fx-padding: 10 20 12 20;");
        VBox rightColumn = new VBox(12, top, statusPanel);
        rightColumn.setPrefWidth(500);
        AnchorPane.setRightAnchor(rightColumn, 24.0);
        AnchorPane.setTopAnchor(rightColumn, 104.0);

        ready.setOnAction(event -> toggleReady());
        ready.setPrefWidth(250);
        AnchorPane.setLeftAnchor(ready, 24.0);
        AnchorPane.setBottomAnchor(ready, 20.0);
        start.setOnAction(event -> startGame());
        start.setPrefWidth(320);
        AnchorPane.setRightAnchor(start, 24.0);
        AnchorPane.setBottomAnchor(start, 20.0);

        root.getChildren().addAll(nameBox, slotGrid, rightColumn, ready, start, title, leave);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        ready.setLoading(false);
        start.setLoading(false);
        onClientStateChanged();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
    }

    @Override
    public void onClientStateChanged() {
        RoomStateDto room = state.getRoom();
        if (room == null) {
            return;
        }
        long me = state.getCurrentUserId();
        status = LobbyStatus.of(room, me);
        roomName.setText(room.roomName());

        for (int slot = 0; slot < slots.size(); slot++) {
            if (slot < room.players().size()) {
                RoomPlayerDto player = room.players().get(slot);
                slots.get(slot).showPlayer(player.username(), player.ready(),
                        player.userId() == room.hostUserId(), player.userId() == me);
            } else {
                slots.get(slot).showEmpty();
            }
        }
        arena.showPlayers(room.players().size());
        playersValue.setText(status.players() + "/" + status.maxPlayers());

        playersCheck.update(status.enoughPlayers() ? ChecklistItem.State.DONE : ChecklistItem.State.NOT_DONE,
                "Players " + status.players() + "/" + status.maxPlayers() + " (min " + ServerRules.MIN_PLAYERS + ")");
        readyCheck.update(status.everyoneReady() ? ChecklistItem.State.DONE : ChecklistItem.State.NOT_DONE,
                "All players ready (" + status.readyPlayers() + "/" + status.players() + ")");
        statusLine.update(status.canStart() ? ChecklistItem.State.DONE : ChecklistItem.State.WAITING,
                status.statusLine());

        if (!ready.isLoading()) {
            ready.setLabelText(status.youReady() ? "UNREADY" : "READY");
            ready.getStyleClass().removeAll("btn-green", "btn-grey");
            ready.getStyleClass().add(status.youReady() ? "btn-grey" : "btn-green");
        }
        if (!start.isLoading()) {
            start.setLabelText(status.host() ? "START GAME" : "WAITING FOR HOST");
            start.setDisable(!status.canStart());
        }
    }

    private void toggleReady() {
        if (status == null) {
            return;
        }
        ready.setLoading(true);
        controller.ready(!status.youReady()).whenComplete((reply, failure) -> {
            ready.setLoading(false);
            onClientStateChanged();
        });
    }

    private void startGame() {
        start.setLoading(true);
        controller.startGame().whenComplete((reply, failure) -> {
            start.setLoading(false);
            onClientStateChanged();
        });
    }

    private static Node infoRow(SvgAssets assets, String iconAsset, String label, Label value) {
        Label name = new Label(label);
        name.getStyleClass().add("info-label");
        value.getStyleClass().add("info-value");
        value.setWrapText(true);
        value.setMinHeight(Label.USE_PREF_SIZE);
        HBox row = new HBox(10, new SvgView(assets, iconAsset, 36, 36), new VBox(0, name, value));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }
}
