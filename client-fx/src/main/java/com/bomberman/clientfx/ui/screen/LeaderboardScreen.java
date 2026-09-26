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
import com.bomberman.common.dto.RankingEntryDto;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** S7 global ranking: top-three podium and a detailed table using server-assigned ranks. */
public final class LeaderboardScreen implements Screen, ClientStateListener {

    private final ClientState state;
    private final GameClientController controller;
    private final SvgAssets assets;
    private final AnchorPane root = new AnchorPane();
    private final HBox podium = new HBox(18);
    private final VBox rows = new VBox(7);
    private final VBox pinnedArea = new VBox(5);
    private final VBox tableArea = new VBox(8);

    public LeaderboardScreen(ClientState state, GameClientController controller,
                             Navigator navigator, SvgAssets assets) {
        this.state = state;
        this.controller = controller;
        this.assets = assets;

        GameButton back = new GameButton("BACK", GameButton.Tone.BLUE, GameButton.Size.M, icon("mdi2a-arrow-left-bold", 24));
        back.setOnAction(event -> navigator.show(ScreenId.HOME));
        AnchorPane.setLeftAnchor(back, 24.0);
        AnchorPane.setTopAnchor(back, 24.0);

        TitleBanner title = TitleBanner.purple("LEADERBOARD", new SvgView(assets, AssetIds.ICON_TROPHY, 52, 52));
        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);
        AnchorPane.setLeftAnchor(titleRow, 0.0);
        AnchorPane.setRightAnchor(titleRow, 0.0);
        AnchorPane.setTopAnchor(titleRow, 18.0);

        podium.setAlignment(Pos.BOTTOM_CENTER);
        podium.setPrefHeight(240);
        AnchorPane.setLeftAnchor(podium, 120.0);
        AnchorPane.setRightAnchor(podium, 120.0);
        AnchorPane.setTopAnchor(podium, 98.0);

        HBox header = new HBox(10,
                header("#", 52), header("PLAYER", 360), header("SCORE", 110),
                header("W", 70), header("D", 70), header("L", 70));
        header.getStyleClass().add("leaderboard-header");
        ScrollPane scroll = new ScrollPane(rows);
        scroll.getStyleClass().add("game-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        tableArea.getChildren().addAll(header, scroll, pinnedArea);
        tableArea.getStyleClass().add("leaderboard-table");
        AnchorPane.setLeftAnchor(tableArea, 52.0);
        AnchorPane.setRightAnchor(tableArea, 52.0);
        AnchorPane.setTopAnchor(tableArea, 350.0);
        AnchorPane.setBottomAnchor(tableArea, 20.0);

        root.getChildren().addAll(podium, tableArea, titleRow, back);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        onClientStateChanged();
        controller.requestRanking();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
    }

    @Override
    public void onClientStateChanged() {
        List<RankingEntryDto> entries = state.getRankingEntries().stream()
                .sorted(Comparator.comparingInt(RankingEntryDto::rank).thenComparingLong(RankingEntryDto::userId))
                .toList();
        podium.getChildren().clear();
        rows.getChildren().clear();
        pinnedArea.getChildren().clear();
        if (entries.isEmpty()) {
            Label empty = new Label("No ranked players yet. Play a match!");
            empty.getStyleClass().add("empty-state");
            podium.getChildren().add(empty);
            tableArea.setVisible(false);
            tableArea.setManaged(false);
            return;
        }
        tableArea.setVisible(true);
        tableArea.setManaged(true);

        List<RankingEntryDto> top = new ArrayList<>(entries.subList(0, Math.min(3, entries.size())));
        // Visual podium order is second, first, third while each block keeps the server rank label.
        if (top.size() >= 2) podium.getChildren().add(podiumEntry(top.get(1), 95));
        podium.getChildren().add(podiumEntry(top.getFirst(), 125));
        if (top.size() >= 3) podium.getChildren().add(podiumEntry(top.get(2), 75));

        List<RankingEntryDto> remainder = entries.subList(top.size(), entries.size());
        remainder.forEach(entry -> rows.getChildren().add(row(entry, false)));
        RankingEntryDto mine = entries.stream()
                .filter(entry -> entry.userId() == state.getCurrentUserId())
                .findFirst().orElse(null);
        if (mine != null && entries.indexOf(mine) >= top.size() + 6) {
            Region divider = new Region();
            divider.getStyleClass().add("pinned-divider");
            pinnedArea.getChildren().addAll(divider, row(mine, true));
        }
    }

    private Node podiumEntry(RankingEntryDto entry, double platformHeight) {
        SvgView avatar = new SvgView(
                assets, AssetIds.BOMBER_FULL, TeamColor.forUser(entry.userId()), 88, 88);
        Label name = new Label(entry.username());
        name.getStyleClass().add("podium-name");
        Label score = new Label(GameFormats.points(entry.totalScoreUnits()) + " PTS");
        score.getStyleClass().add("podium-score");
        Label rank = new Label(Integer.toString(entry.rank()));
        rank.getStyleClass().add("podium-rank");
        VBox platform = new VBox(rank);
        platform.getStyleClass().addAll("podium-platform", "podium-rank-" + Math.min(entry.rank(), 4));
        platform.setAlignment(Pos.TOP_CENTER);
        platform.setPrefSize(190, platformHeight);
        VBox item = new VBox(2, avatar, name, score, platform);
        item.setAlignment(Pos.BOTTOM_CENTER);
        item.setPrefWidth(190);
        return item;
    }

    private Node row(RankingEntryDto entry, boolean pinned) {
        HBox row = new HBox(10);
        row.getStyleClass().add("leaderboard-row");
        if (entry.userId() == state.getCurrentUserId()) row.getStyleClass().add("you");
        if (pinned) row.getStyleClass().add("pinned");
        row.getChildren().addAll(
                value("#" + entry.rank(), 52, "leaderboard-rank"),
                player(entry),
                value(GameFormats.points(entry.totalScoreUnits()), 110, "leaderboard-score"),
                value(Integer.toString(entry.totalWins()), 70, null),
                value(Integer.toString(entry.totalDraws()), 70, null),
                value(Integer.toString(entry.totalLosses()), 70, null)
        );
        return row;
    }

    private Node player(RankingEntryDto entry) {
        Label name = new Label(entry.username());
        name.getStyleClass().add("leaderboard-name");
        HBox player = new HBox(10, new PlayerHead(assets, TeamColor.forUser(entry.userId()), 42, true), name);
        player.setAlignment(Pos.CENTER_LEFT);
        player.setPrefWidth(360);
        if (entry.userId() == state.getCurrentUserId()) player.getChildren().add(new Tag("YOU", Tag.Kind.YOU));
        return player;
    }

    private static Label header(String text, double width) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        return label;
    }

    private static Label value(String text, double width, String style) {
        Label label = new Label(text);
        label.setPrefWidth(width);
        label.setAlignment(Pos.CENTER_LEFT);
        if (style != null) label.getStyleClass().add(style);
        return label;
    }

    private static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }
}
