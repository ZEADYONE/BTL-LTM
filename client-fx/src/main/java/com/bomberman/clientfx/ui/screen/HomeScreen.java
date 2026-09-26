package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.game.QuickPlay;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.HeroCharacter;
import com.bomberman.clientfx.ui.component.MenuTileButton;
import com.bomberman.clientfx.ui.component.OutlinedText;
import com.bomberman.clientfx.ui.component.Pill;
import com.bomberman.clientfx.ui.component.PlayerBadge;
import com.bomberman.clientfx.ui.component.StatusDot;
import com.bomberman.clientfx.ui.component.SvgView;
import com.bomberman.clientfx.ui.popup.HelpPopup;
import com.bomberman.clientfx.ui.popup.SettingsPopup;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.dto.RankingEntryDto;
import com.bomberman.common.dto.RoomSummaryDto;
import com.bomberman.common.enums.MessageType;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** S2 Home (mockup 1, elements H-01…H-12 in docs/ui-redesign/01). */
public final class HomeScreen implements Screen, ClientStateListener {

    private final ClientState state;
    private final GameClientController controller;
    private final AnchorPane root = new AnchorPane();
    private final PlayerBadge badge;
    private final Pill scorePill;
    private final Pill onlinePill;
    private final HeroCharacter hero;
    private final Label roomsSubtitle = new Label();
    private final GameButton play;

    public HomeScreen(ClientState state, GameClientController controller, Navigator navigator,
                      SvgAssets assets, AppShell shell, Stage stage) {
        this.state = state;
        this.controller = controller;

        badge = new PlayerBadge(assets);
        AnchorPane.setLeftAnchor(badge, 24.0);
        AnchorPane.setTopAnchor(badge, 20.0);

        scorePill = new Pill(new SvgView(assets, AssetIds.ICON_STAR, 32, 32), "0");
        onlinePill = new Pill(new StatusDot(true), "0 ONLINE");
        onlinePill.setCursor(Cursor.HAND);
        onlinePill.setOnMouseClicked(event -> navigator.show(ScreenId.ROOM_BROWSER));
        HBox pills = new HBox(14, scorePill, onlinePill);
        AnchorPane.setRightAnchor(pills, 24.0);
        AnchorPane.setTopAnchor(pills, 26.0);

        MenuTileButton leaderboard = new MenuTileButton("LEADERBOARD", assets, AssetIds.ICON_TROPHY);
        leaderboard.setOnAction(event -> navigator.show(ScreenId.LEADERBOARD));
        MenuTileButton history = new MenuTileButton("HISTORY", assets, AssetIds.ICON_HISTORY);
        history.setOnAction(event -> navigator.show(ScreenId.HISTORY));
        MenuTileButton settings = new MenuTileButton("SETTINGS", assets, AssetIds.ICON_GEAR);
        settings.setOnAction(event -> SettingsPopup.show(shell, stage, state, controller, navigator));
        MenuTileButton help = new MenuTileButton("HELP", assets, AssetIds.ICON_HELP);
        help.setOnAction(event -> HelpPopup.show(shell));
        VBox menu = new VBox(26, leaderboard, history, settings, help);
        AnchorPane.setLeftAnchor(menu, 44.0);
        AnchorPane.setTopAnchor(menu, 150.0);

        hero = new HeroCharacter(assets, TeamColor.RED, 400);
        AnchorPane.setLeftAnchor(hero, 400.0);
        AnchorPane.setTopAnchor(hero, 70.0);

        GameButton rooms = new GameButton("", GameButton.Tone.PURPLE, GameButton.Size.L, roomsCardContent());
        rooms.getStyleClass().add("rooms-card");
        rooms.setOnAction(event -> navigator.show(ScreenId.ROOM_BROWSER));
        play = new GameButton("PLAY", GameButton.Tone.YELLOW, GameButton.Size.XL,
                new SvgView(assets, AssetIds.BOMB, 78, 78));
        play.getStyleClass().add("play-button");
        play.setOnAction(event -> quickPlay());
        HBox bottom = new HBox(20, rooms, play);
        bottom.setAlignment(Pos.BOTTOM_RIGHT);
        AnchorPane.setRightAnchor(bottom, 20.0);
        AnchorPane.setBottomAnchor(bottom, 20.0);

        root.getChildren().addAll(hero, badge, pills, menu, bottom);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        play.setLoading(false);
        hero.setTeam(TeamColor.forUser(state.getCurrentUserId()));
        hero.play();
        onClientStateChanged();
        controller.requestLobbyState();
        controller.requestRanking();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
        hero.stop();
    }

    @Override
    public void onClientStateChanged() {
        Optional<RankingEntryDto> mine = state.getRankingEntries().stream()
                .filter(entry -> entry.userId() == state.getCurrentUserId())
                .findFirst();
        String subtitle = mine
                .filter(entry -> entry.totalWins() + entry.totalDraws() + entry.totalLosses() > 0)
                .map(entry -> "RANK #" + entry.rank() + " · " + points(entry.totalScoreUnits()) + " PTS")
                .orElse("UNRANKED");
        badge.setPlayer(
                state.isLoggedIn() ? state.getCurrentUsername() : "-",
                TeamColor.forUser(state.getCurrentUserId()),
                subtitle
        );
        scorePill.setValue(points(mine.map(RankingEntryDto::totalScoreUnits).orElse(0L)));
        int online = state.getOnlineUsers().size();
        onlinePill.setValue(online + " ONLINE");
        int open = QuickPlay.candidates(state.getRooms()).size();
        roomsSubtitle.setText(open + " OPEN · " + online + " ONLINE");
    }

    /** H-11: join the fullest open room, retry once on a race, otherwise create a room. */
    private void quickPlay() {
        play.setLoading(true);
        tryJoin(QuickPlay.candidates(state.getRooms()), 0);
    }

    private void tryJoin(List<RoomSummaryDto> candidates, int attempt) {
        if (attempt >= candidates.size() || attempt >= QuickPlay.MAX_JOIN_ATTEMPTS) {
            controller.createRoom(QuickPlay.defaultRoomName(state.getCurrentUsername()))
                    .whenComplete((reply, failure) -> play.setLoading(false));
            return;
        }
        controller.joinRoom(candidates.get(attempt).roomId(), true).whenComplete((reply, failure) -> {
            if (reply != null && reply.type() == MessageType.ERROR) {
                String code = reply.payload().path("code").asText();
                if (QuickPlay.isRetryable(code)) {
                    tryJoin(candidates, attempt + 1);
                    return;
                }
                state.setFeedback(Feedback.error(reply.payload().path("message").asText(code)));
            }
            play.setLoading(false);
        });
    }

    private Node roomsCardContent() {
        OutlinedText heading = new OutlinedText("ONLINE ROOMS", OutlinedText.Style.BUTTON_L);
        roomsSubtitle.getStyleClass().add("rooms-subtitle");
        VBox text = new VBox(2, heading, roomsSubtitle);
        text.setAlignment(Pos.CENTER_LEFT);
        FontIcon chevron = new FontIcon("mdi2c-chevron-right");
        chevron.setIconSize(40);
        HBox content = new HBox(16, miniMap(), text, chevron);
        content.setAlignment(Pos.CENTER_LEFT);
        return content;
    }

    /** Tiny arena thumbnail for the rooms card (C05 fallback drawn in code). */
    private static Node miniMap() {
        String[] rows = {"#####", "#.c.#", "#.#b#", "#####"};
        GridPane grid = new GridPane();
        grid.getStyleClass().add("mini-map");
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length(); column++) {
                Color fill = switch (rows[row].charAt(column)) {
                    case '#' -> Color.web("#B8BCC8");
                    case 'c' -> Color.web("#E08A3C");
                    case 'b' -> Color.web("#3A3550");
                    default -> (row + column) % 2 == 0 ? Color.web("#7CC04B") : Color.web("#72B545");
                };
                Rectangle cell = new Rectangle(14, 14, fill);
                cell.setStroke(Color.web("#2A1F3D", 0.35));
                grid.add(cell, column, row);
            }
        }
        return grid;
    }

    private static String points(long scoreUnits) {
        return String.format(Locale.ROOT, "%.1f", scoreUnits / 2.0);
    }
}
