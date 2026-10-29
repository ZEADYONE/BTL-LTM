package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.game.GameFormats;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.PlayerHead;
import com.bomberman.clientfx.ui.component.SvgView;
import com.bomberman.clientfx.ui.component.Tag;
import com.bomberman.clientfx.ui.component.TitleBanner;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.dto.MatchHistoryEntryDto;
import com.bomberman.common.dto.MatchHistoryPlayerDto;
import com.bomberman.common.enums.GameResult;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/** S8 match history, newest first and formatted in the machine's local time zone. */
public final class HistoryScreen implements Screen, ClientStateListener {

    private final ClientState state;
    private final GameClientController controller;
    private final SvgAssets assets;
    private final AnchorPane root = new AnchorPane();
    private final VBox cards = new VBox(10);
    private final StackPaneState content = new StackPaneState();

    public HistoryScreen(ClientState state, GameClientController controller,
                         Navigator navigator, SvgAssets assets) {
        this.state = state;
        this.controller = controller;
        this.assets = assets;

        GameButton back = new GameButton("BACK", GameButton.Tone.BLUE, GameButton.Size.M, icon("mdi2a-arrow-left-bold", 24));
        back.setOnAction(event -> navigator.show(ScreenId.HOME));
        AnchorPane.setLeftAnchor(back, 24.0);
        AnchorPane.setTopAnchor(back, 24.0);

        TitleBanner title = TitleBanner.purple("MATCH HISTORY", new SvgView(assets, AssetIds.ICON_HISTORY, 52, 52));
        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);
        AnchorPane.setLeftAnchor(titleRow, 0.0);
        AnchorPane.setRightAnchor(titleRow, 0.0);
        AnchorPane.setTopAnchor(titleRow, 18.0);

        ScrollPane scroll = new ScrollPane(cards);
        scroll.getStyleClass().add("game-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        content.setContent(scroll, "No matches yet. Go play!");
        AnchorPane.setLeftAnchor(content.root, 34.0);
        AnchorPane.setRightAnchor(content.root, 34.0);
        AnchorPane.setTopAnchor(content.root, 112.0);
        AnchorPane.setBottomAnchor(content.root, 24.0);
        root.getChildren().addAll(content.root, titleRow, back);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        onClientStateChanged();
        controller.requestHistory();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
    }

    @Override
    public void onClientStateChanged() {
        List<MatchHistoryEntryDto> entries = state.getMatchHistory().stream()
                .sorted(Comparator.comparingLong(MatchHistoryEntryDto::endedAtEpochMillis).reversed())
                .toList();
        cards.getChildren().setAll(entries.stream().map(this::card).toList());
        content.showEmpty(entries.isEmpty());
    }

    private Node card(MatchHistoryEntryDto match) {
        Label result = new Label(switch (match.viewerResult()) {
            case WIN -> "VICTORY";
            case LOSS -> "DEFEAT";
            case DRAW -> "DRAW";
        });
        result.getStyleClass().add("history-result");
        Label time = new Label(GameFormats.localTime(match.endedAtEpochMillis(), ZoneId.systemDefault()));
        time.getStyleClass().add("history-meta");
        Label duration = new Label(GameFormats.duration(match.startedAtEpochMillis(), match.endedAtEpochMillis()));
        duration.getStyleClass().add("history-duration");
        VBox summary = new VBox(2, result, time, duration);
        summary.setPrefWidth(230);

        HBox players = new HBox(10);
        players.setAlignment(Pos.CENTER_LEFT);
        for (int slot = 0; slot < match.players().size(); slot++) {
            players.getChildren().add(participant(match.players().get(slot), slot, match.winnerUserId()));
        }
        HBox.setHgrow(players, Priority.ALWAYS);

        Label score = new Label(GameFormats.earnedPoints(match.viewerScoreEarnedUnits()));
        score.getStyleClass().add("history-score");
        HBox row = new HBox(16, summary, players, score);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("history-card", "history-" + match.viewerResult().name().toLowerCase());
        return row;
    }

    private Node participant(MatchHistoryPlayerDto player, int slot, Long winnerUserId) {
        PlayerHead head = new PlayerHead(assets, TeamColor.forSlot(slot), 42, true);
        Label name = new Label(player.username());
        name.getStyleClass().add("history-player-name");
        VBox participant = new VBox(1, head, name);
        participant.setAlignment(Pos.CENTER);
        participant.setPrefWidth(112);
        if (winnerUserId != null && winnerUserId == player.userId()) {
            participant.getChildren().add(new Tag(
                    "WINNER", Tag.Kind.WINNER, new SvgView(assets, AssetIds.ICON_CROWN, 18, 18)));
        }
        return participant;
    }

    private static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }

    /** A tiny local helper that overlays a message without adding another reusable component. */
    private static final class StackPaneState {
        private final javafx.scene.layout.StackPane root = new javafx.scene.layout.StackPane();
        private final Label empty = new Label();

        private void setContent(Node node, String emptyText) {
            empty.setText(emptyText);
            empty.getStyleClass().add("empty-state");
            root.getChildren().setAll(node, empty);
        }

        private void showEmpty(boolean show) {
            empty.setVisible(show);
            empty.setManaged(show);
        }
    }
}
