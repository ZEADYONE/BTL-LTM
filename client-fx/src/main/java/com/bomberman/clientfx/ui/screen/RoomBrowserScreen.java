package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.GameFields;
import com.bomberman.clientfx.ui.component.Panel;
import com.bomberman.clientfx.ui.component.RoomCard;
import com.bomberman.clientfx.ui.component.SvgView;
import com.bomberman.clientfx.ui.component.TitleBanner;
import com.bomberman.common.dto.OnlineUserDto;
import com.bomberman.common.dto.RoomSummaryDto;
import com.bomberman.common.enums.PlayerStatus;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bomberman.clientfx.ui.InputValidation.roomNameError;

/** S3 Room Browser (no mockup; docs/ui-redesign/02, section 3.3). */
public final class RoomBrowserScreen implements Screen, ClientStateListener {

    private static final double CARD_WIDTH = 330;

    private final ClientState state;
    private final GameClientController controller;
    private final SvgAssets assets;
    private final AnchorPane root = new AnchorPane();
    private final TilePane grid = new TilePane(16, 16);
    private final Map<String, RoomCard> cards = new HashMap<>();
    private final Label emptyRooms = new Label("No rooms yet. Create one!");
    private final StackPane roomArea;
    private final TextField roomName = GameFields.text("Room name");
    private final Label roomNameError = new Label();
    private final GameButton create = new GameButton("CREATE", GameButton.Tone.YELLOW, GameButton.Size.M);
    private final VBox onlineList = new VBox(8);
    private final Panel onlinePanel;

    public RoomBrowserScreen(ClientState state, GameClientController controller, Navigator navigator, SvgAssets assets) {
        this.state = state;
        this.controller = controller;
        this.assets = assets;

        GameButton back = new GameButton("BACK", GameButton.Tone.BLUE, GameButton.Size.M, icon("mdi2a-arrow-left-bold", 24));
        back.setOnAction(event -> navigator.show(ScreenId.HOME));
        AnchorPane.setLeftAnchor(back, 24.0);
        AnchorPane.setTopAnchor(back, 24.0);
        TitleBanner title = TitleBanner.purple("ONLINE ROOMS", new SvgView(assets, AssetIds.ICON_PLAYERS, 52, 52));
        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);
        AnchorPane.setLeftAnchor(titleRow, 0.0);
        AnchorPane.setRightAnchor(titleRow, 0.0);
        AnchorPane.setTopAnchor(titleRow, 18.0);

        grid.setPrefColumns(2);
        grid.setPrefTileWidth(CARD_WIDTH);
        grid.setTileAlignment(Pos.TOP_LEFT);
        ScrollPane scroll = new ScrollPane(grid);
        scroll.getStyleClass().add("game-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        emptyRooms.getStyleClass().add("empty-state");
        roomArea = new StackPane(scroll, emptyRooms);
        AnchorPane.setLeftAnchor(roomArea, 24.0);
        AnchorPane.setTopAnchor(roomArea, 120.0);
        AnchorPane.setBottomAnchor(roomArea, 24.0);
        roomArea.setPrefWidth(CARD_WIDTH * 2 + 16 + 12);

        roomName.setOnAction(event -> createRoom());
        roomNameError.getStyleClass().add("field-error");
        roomNameError.managedProperty().bind(roomNameError.textProperty().isNotEmpty());
        roomName.textProperty().addListener(observable -> roomNameError.setText(""));
        create.setOnAction(event -> createRoom());
        create.setMaxWidth(Double.MAX_VALUE);
        Panel createPanel = new Panel(Panel.Style.CREAM, "CREATE ROOM", roomName, roomNameError, create);

        ScrollPane onlineScroll = new ScrollPane(onlineList);
        onlineScroll.getStyleClass().add("game-scroll");
        onlineScroll.setFitToWidth(true);
        onlineScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(onlineScroll, Priority.ALWAYS);
        onlinePanel = new Panel(Panel.Style.PURPLE, "ONLINE", onlineScroll);
        VBox.setVgrow(onlinePanel, Priority.ALWAYS);
        VBox.setVgrow(onlinePanel.body(), Priority.ALWAYS);

        VBox side = new VBox(16, createPanel, onlinePanel);
        side.setPrefWidth(360);
        AnchorPane.setRightAnchor(side, 24.0);
        AnchorPane.setTopAnchor(side, 120.0);
        AnchorPane.setBottomAnchor(side, 24.0);

        root.getChildren().addAll(roomArea, side, titleRow, back);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        create.setLoading(false);
        roomNameError.setText("");
        onClientStateChanged();
        controller.requestLobbyState();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
    }

    @Override
    public void onClientStateChanged() {
        List<RoomSummaryDto> rooms = state.getRooms();
        cards.keySet().retainAll(rooms.stream().map(RoomSummaryDto::roomId).toList());
        grid.getChildren().clear();
        for (RoomSummaryDto room : rooms) {
            RoomCard card = cards.computeIfAbsent(room.roomId(), id -> newCard(id));
            card.update(room.roomName(), hostName(room.hostUserId()).orElse(null),
                    room.playerCount(), room.maxPlayers(), room.status());
            grid.getChildren().add(card);
        }
        emptyRooms.setVisible(rooms.isEmpty());

        List<OnlineUserDto> online = state.getOnlineUsers();
        onlinePanel.setTitle("ONLINE (" + online.size() + ")");
        onlineList.getChildren().setAll(online.stream().map(this::onlineRow).toList());
    }

    private RoomCard newCard(String roomId) {
        RoomCard card = new RoomCard(assets);
        card.setPrefWidth(CARD_WIDTH);
        card.setOnJoin(() -> {
            card.setBusy(true);
            controller.joinRoom(roomId).whenComplete((reply, failure) -> card.setBusy(false));
        });
        return card;
    }

    private void createRoom() {
        Optional<String> problem = roomNameError(roomName.getText());
        GameFields.markError(roomName, problem.isPresent());
        roomNameError.setText(problem.orElse(""));
        if (problem.isPresent() || create.isLoading()) {
            return;
        }
        create.setLoading(true);
        controller.createRoom(roomName.getText().strip()).whenComplete((reply, failure) -> create.setLoading(false));
    }

    private Optional<String> hostName(long hostUserId) {
        return state.getOnlineUsers().stream()
                .filter(user -> user.userId() == hostUserId)
                .map(OnlineUserDto::username)
                .findFirst();
    }

    private Node onlineRow(OnlineUserDto user) {
        Circle dot = new Circle(7, statusColour(user.status()));
        dot.setStroke(Color.web("#2A1F3D"));
        dot.setStrokeWidth(2);
        String suffix = user.userId() == state.getCurrentUserId() ? " (you)" : "";
        Label name = new Label(user.username() + suffix);
        name.getStyleClass().add("online-name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label status = new Label(statusText(user.status()));
        status.getStyleClass().add("online-status");
        HBox row = new HBox(10, dot, name, spacer, status);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Color statusColour(PlayerStatus status) {
        return switch (status) {
            case FREE -> Color.web("#43C463");
            case IN_ROOM -> Color.web("#F5B82E");
            case PLAYING -> Color.web("#E84B4B");
        };
    }

    private static String statusText(PlayerStatus status) {
        return switch (status) {
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
