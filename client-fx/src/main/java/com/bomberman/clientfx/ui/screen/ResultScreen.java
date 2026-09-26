package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.game.GameFormats;
import com.bomberman.clientfx.game.MatchTracker;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.Confetti;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.OutlinedText;
import com.bomberman.clientfx.ui.component.RankRow;
import com.bomberman.clientfx.ui.component.SunRays;
import com.bomberman.clientfx.ui.component.SvgView;
import com.bomberman.clientfx.ui.component.Tag;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameOverPlayerDto;
import com.bomberman.common.enums.GameResult;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.enums.RoomStatus;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;

import java.util.List;

/** S6 match result with VICTORY, DEFEAT and DRAW variants. */
public final class ResultScreen implements Screen, ClientStateListener {

    private final ClientState state;
    private final GameClientController controller;
    private final Navigator navigator;
    private final SvgAssets assets;
    private final MatchTracker tracker;
    private final AnchorPane root = new AnchorPane();
    private final StackPane titleHolder = new StackPane();
    private final SunRays rays = new SunRays(210);
    private final SvgView character;
    private final SvgView trophy;
    private final SvgView skull;
    private final VBox ranking = new VBox(8);
    private final Confetti confetti = new Confetti();
    private final GameButton playAgain = new GameButton("PLAY AGAIN", GameButton.Tone.YELLOW, GameButton.Size.L);
    private final GameButton home = new GameButton("HOME", GameButton.Tone.BLUE, GameButton.Size.L);
    private boolean playAgainPending;
    private boolean rematchAvailable;

    public ResultScreen(ClientState state, GameClientController controller, Navigator navigator,
                        SvgAssets assets, MatchTracker tracker) {
        this.state = state;
        this.controller = controller;
        this.navigator = navigator;
        this.assets = assets;
        this.tracker = tracker;
        root.getStyleClass().add("result-screen");

        AnchorPane.setLeftAnchor(titleHolder, 0.0);
        AnchorPane.setRightAnchor(titleHolder, 0.0);
        AnchorPane.setTopAnchor(titleHolder, 12.0);

        character = new SvgView(assets, AssetIds.BOMBER_FULL, TeamColor.RED, 360, 360);
        trophy = new SvgView(assets, AssetIds.ICON_TROPHY, 110, 110);
        trophy.setTranslateX(125);
        trophy.setTranslateY(-82);
        skull = new SvgView(assets, AssetIds.ICON_SKULL, 96, 96);
        skull.setTranslateY(-105);
        StackPane hero = new StackPane(rays, character, trophy, skull);
        hero.setPrefSize(470, 440);
        AnchorPane.setLeftAnchor(hero, 24.0);
        AnchorPane.setTopAnchor(hero, 102.0);

        Label tableTitle = new Label("MATCH RANKING");
        tableTitle.getStyleClass().add("section-heading");
        ScrollPane rankingScroll = new ScrollPane(ranking);
        rankingScroll.getStyleClass().add("game-scroll");
        rankingScroll.setFitToWidth(true);
        rankingScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rankingScroll.setPrefViewportHeight(330);
        VBox resultTable = new VBox(10, tableTitle, rankingScroll);
        resultTable.getStyleClass().add("result-table");
        resultTable.setPrefWidth(590);
        AnchorPane.setRightAnchor(resultTable, 24.0);
        AnchorPane.setTopAnchor(resultTable, 104.0);

        playAgain.setPrefWidth(285);
        playAgain.setOnAction(event -> requestRematch());
        home.setPrefWidth(220);
        home.setOnAction(event -> leaveRoom());
        HBox buttons = new HBox(16, home, playAgain);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        AnchorPane.setRightAnchor(buttons, 24.0);
        AnchorPane.setBottomAnchor(buttons, 18.0);

        AnchorPane.setTopAnchor(confetti, 0.0);
        AnchorPane.setBottomAnchor(confetti, 0.0);
        AnchorPane.setLeftAnchor(confetti, 0.0);
        AnchorPane.setRightAnchor(confetti, 0.0);
        root.getChildren().addAll(hero, resultTable, buttons, titleHolder, confetti);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(this);
        playAgainPending = false;
        rematchAvailable = false;
        playAgain.setLoading(false);
        playAgain.setLabelText("PLAY AGAIN");
        home.setLoading(false);
        confetti.stopAndClear();
        renderResult();
        onClientStateChanged();
    }

    @Override
    public void onHide() {
        state.removeListener(this);
        rays.stop();
        confetti.stopAndClear();
    }

    @Override
    public void onClientStateChanged() {
        if (state.getRoom() != null
                && state.getRoom().status() == RoomStatus.WAITING
                && !playAgainPending
                && !rematchAvailable) {
            rematchAvailable = true;
            playAgain.setLoading(false);
            playAgain.setLabelText("BACK TO ROOM");
            state.setFeedback(Feedback.info("Room is ready for a rematch."));
        }
    }

    private void renderResult() {
        GameOverDto gameOver = state.getGameOver();
        GameOverPlayerDto mine = gameOver == null ? null : gameOver.players().stream()
                .filter(player -> player.userId() == state.getCurrentUserId())
                .findFirst().orElse(null);
        GameResult myResult = mine == null ? GameResult.DRAW : mine.result();
        OutlinedText title = new OutlinedText(title(myResult), OutlinedText.Style.DISPLAY, fill(myResult));
        if (myResult == GameResult.WIN) {
            HBox crownedTitle = new HBox(14, new SvgView(assets, AssetIds.ICON_CROWN, 74, 74), title);
            crownedTitle.setAlignment(Pos.CENTER);
            titleHolder.getChildren().setAll(crownedTitle);
        } else {
            titleHolder.getChildren().setAll(title);
        }

        int mySlot = tracker.slotFor(state.getCurrentUserId()).orElse(0);
        character.setTeam(TeamColor.forSlot(mySlot));
        character.setOpacity(myResult == GameResult.LOSS ? 0.58 : 1);
        character.setRotate(myResult == GameResult.LOSS ? -7 : 0);
        trophy.setVisible(myResult == GameResult.WIN);
        skull.setVisible(myResult == GameResult.LOSS);
        rays.setVisible(myResult != GameResult.LOSS);
        if (myResult != GameResult.LOSS) rays.play(); else rays.stop();
        if (myResult == GameResult.WIN) Platform.runLater(confetti::burst);

        ranking.getChildren().clear();
        if (gameOver == null) {
            ranking.getChildren().add(empty("Waiting for match result…"));
            return;
        }
        List<MatchTracker.RankedPlayer> ranked = tracker.rank(gameOver);
        for (MatchTracker.RankedPlayer player : ranked) {
            Tag badge = switch (player.result()) {
                case WIN -> new Tag("WINNER", Tag.Kind.WINNER);
                case DRAW -> new Tag("DRAW", Tag.Kind.DRAW);
                case LOSS -> null;
            };
            ranking.getChildren().add(new RankRow(
                    assets,
                    player.rank(),
                    player.username(),
                    TeamColor.forSlot(player.slot()),
                    GameFormats.earnedPoints(player.scoreEarnedUnits()),
                    badge,
                    player.userId() == state.getCurrentUserId()
            ));
        }
    }

    private void requestRematch() {
        if (rematchAvailable) {
            navigator.show(ScreenId.ROOM_LOBBY);
            return;
        }
        if (playAgainPending) return;
        playAgainPending = true;
        playAgain.setLoading(true);
        controller.playAgain().whenComplete((reply, failure) -> {
            playAgainPending = false;
            playAgain.setLoading(false);
            if (failure != null || reply == null) return;
            if (reply.type() == MessageType.ERROR) {
                String code = reply.payload().path("code").asText();
                if (!"ROOM_NOT_FINISHED".equals(code)) {
                    state.setFeedback(Feedback.error(reply.payload().path("message").asText(code)));
                    return;
                }
            }
            navigator.show(ScreenId.ROOM_LOBBY);
        });
    }

    private void leaveRoom() {
        home.setLoading(true);
        controller.leaveRoom().whenComplete((reply, failure) -> {
            home.setLoading(false);
        });
    }

    private static String title(GameResult result) {
        return switch (result) {
            case WIN -> "VICTORY";
            case LOSS -> "DEFEAT";
            case DRAW -> "DRAW";
        };
    }

    private static Paint fill(GameResult result) {
        return switch (result) {
            case WIN -> OutlinedText.GOLD;
            case LOSS -> OutlinedText.STEEL;
            case DRAW -> OutlinedText.SILVER;
        };
    }

    private static Label empty(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        return label;
    }
}
