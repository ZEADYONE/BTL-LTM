package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.game.GameRenderer;
import com.bomberman.clientfx.game.InputController;
import com.bomberman.clientfx.game.MatchTracker;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.HudPlayerCard;
import com.bomberman.clientfx.ui.popup.MatchMenuPopup;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.GameStatus;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** S5 live match: Canvas world, JavaFX HUD, held input and the ESC match menu. */
public final class GameScreen implements Screen {

    private static final long RESULT_DELAY_NANOS = 1_200_000_000L;
    private static final double[] HUD_X = {16, 250, 808, 1042};

    private final ClientState state;
    private final GameClientController controller;
    private final Navigator navigator;
    private final AppShell shell;
    private final SvgAssets assets;
    private final StackPane root = new StackPane();
    private final Canvas canvas = new Canvas(AppShell.DESIGN_WIDTH, AppShell.DESIGN_HEIGHT);
    private final AnchorPane hud = new AnchorPane();
    private final Label clock = new Label("00:00");
    private final Label outBanner = new Label("YOU'RE OUT  ·  watching the match");
    private final Label diagnostics = new Label();
    private final List<HudPlayerCard> cards = new ArrayList<>();
    private final Set<KeyCode> heldKeys = EnumSet.noneOf(KeyCode.class);
    private final MatchTracker tracker;
    private final GameRenderer renderer;
    private final InputController input;
    private final AnimationTimer timer;
    private final ChangeListener<Boolean> focusListener = (observable, hadFocus, hasFocus) -> {
        if (!hasFocus) clearKeys();
    };
    private final EventHandler<KeyEvent> keyPressedHandler = this::keyPressed;
    private final EventHandler<KeyEvent> keyReleasedHandler = this::keyReleased;
    private Scene attachedScene;
    private Window attachedWindow;
    private GameStateDto renderedSnapshot;
    private long lastFrameNanos;
    private long gameOverStartedNanos = Long.MIN_VALUE;
    private long fpsWindowNanos;
    private int fpsFrames;
    private double displayedFps;

    public GameScreen(ClientState state, GameClientController controller, Navigator navigator,
                      SvgAssets assets, AppShell shell, MatchTracker tracker) {
        this.state = state;
        this.controller = controller;
        this.navigator = navigator;
        this.shell = shell;
        this.assets = assets;
        this.tracker = tracker;
        renderer = new GameRenderer(canvas, assets, tracker);
        input = new InputController(controller::move, controller::placeBomb, this::inputAllowed);

        root.setMinSize(AppShell.DESIGN_WIDTH, AppShell.DESIGN_HEIGHT);
        root.setPrefSize(AppShell.DESIGN_WIDTH, AppShell.DESIGN_HEIGHT);
        root.setMaxSize(AppShell.DESIGN_WIDTH, AppShell.DESIGN_HEIGHT);
        root.getStyleClass().add("game-screen");

        hud.setPickOnBounds(false);
        clock.getStyleClass().add("match-clock");
        AnchorPane.setTopAnchor(clock, 18.0);
        AnchorPane.setLeftAnchor(clock, AppShell.DESIGN_WIDTH / 2 - 70);
        clock.setPrefWidth(140);
        clock.setAlignment(Pos.CENTER);
        hud.getChildren().add(clock);

        outBanner.getStyleClass().add("out-banner");
        outBanner.setVisible(false);
        outBanner.setManaged(false);
        StackPane.setAlignment(outBanner, Pos.BOTTOM_CENTER);
        outBanner.setTranslateY(-16);

        diagnostics.getStyleClass().add("fps-overlay");
        diagnostics.setVisible(false);
        diagnostics.setManaged(false);
        StackPane.setAlignment(diagnostics, Pos.BOTTOM_RIGHT);
        diagnostics.setTranslateX(-14);
        diagnostics.setTranslateY(-14);

        root.getChildren().addAll(canvas, hud, outBanner, diagnostics);
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                frame(now);
            }
        };
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public boolean framed() {
        return false;
    }

    @Override
    public void onShow() {
        tracker.reset();
        renderer.reset();
        cards.forEach(card -> hud.getChildren().remove(card));
        cards.clear();
        clock.setText("00:00");
        outBanner.setVisible(false);
        outBanner.setManaged(false);
        renderedSnapshot = null;
        lastFrameNanos = 0;
        gameOverStartedNanos = Long.MIN_VALUE;
        fpsWindowNanos = 0;
        fpsFrames = 0;
        clearKeys();
        attachInput();
        timer.start();
        state.setFeedback(Feedback.info("WASD / Arrows: move · SPACE: bomb · ESC: menu"));
        root.requestFocus();
    }

    @Override
    public void onHide() {
        timer.stop();
        clearKeys();
        detachInput();
        shell.closeModal();
    }

    private void frame(long nowNanos) {
        double elapsed = lastFrameNanos == 0 ? 0 : Math.min(0.05, (nowNanos - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = nowNanos;

        GameStateDto current = state.getGameState();
        if (current != null && current != renderedSnapshot) {
            MatchTracker.FrameEvents events = tracker.accept(current);
            renderer.acceptSnapshot(current, events, nowNanos);
            renderedSnapshot = current;
            updateHud(current, events.newMatch());
        }

        input.update(nowNanos);
        GameOverDto gameOver = state.getGameOver();
        if (gameOver != null) {
            renderer.showWinner(gameOver.winnerUserId(), nowNanos);
            if (gameOverStartedNanos == Long.MIN_VALUE) {
                gameOverStartedNanos = nowNanos;
            } else if (nowNanos - gameOverStartedNanos >= RESULT_DELAY_NANOS) {
                navigator.show(ScreenId.RESULT);
                return;
            }
        }

        renderer.render(nowNanos, elapsed);
        updateDiagnostics(nowNanos);
    }

    private void updateHud(GameStateDto game, boolean newMatch) {
        if (newMatch || cards.size() != game.players().size()) {
            cards.forEach(card -> hud.getChildren().remove(card));
            cards.clear();
            for (int slot = 0; slot < game.players().size(); slot++) {
                HudPlayerCard card = new HudPlayerCard(assets, slot, game.players().get(slot).username());
                card.setLayoutX(HUD_X[slot]);
                card.setLayoutY(14);
                hud.getChildren().add(card);
                cards.add(card);
            }
        }
        for (int slot = 0; slot < game.players().size(); slot++) {
            GamePlayerStateDto player = game.players().get(slot);
            HudPlayerCard card = cards.get(slot);
            card.update(Math.max(0, player.bombCapacity() - player.activeBombs()), player.bombRange(), player.alive());
        }
        long seconds = Math.max(0, game.tick() / ServerRules.TICKS_PER_SECOND);
        clock.setText("%02d:%02d".formatted(seconds / 60, seconds % 60));
        GamePlayerStateDto me = game.players().stream()
                .filter(player -> player.userId() == state.getCurrentUserId())
                .findFirst().orElse(null);
        boolean out = me != null && !me.alive();
        outBanner.setVisible(out);
        outBanner.setManaged(out);
    }

    private boolean inputAllowed() {
        GameStateDto game = state.getGameState();
        if (game == null || game.gameStatus() != GameStatus.RUNNING || shell.isModalShowing()) {
            return false;
        }
        return game.players().stream()
                .anyMatch(player -> player.userId() == state.getCurrentUserId() && player.alive());
    }

    private void attachInput() {
        attachedScene = root.getScene();
        if (attachedScene != null) {
            attachedScene.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
            attachedScene.addEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
            attachedWindow = attachedScene.getWindow();
            if (attachedWindow != null) {
                attachedWindow.focusedProperty().addListener(focusListener);
            }
        }
    }

    private void detachInput() {
        if (attachedScene != null) {
            attachedScene.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
            attachedScene.removeEventFilter(KeyEvent.KEY_RELEASED, keyReleasedHandler);
        }
        if (attachedWindow != null) {
            attachedWindow.focusedProperty().removeListener(focusListener);
        }
        attachedScene = null;
        attachedWindow = null;
    }

    private void keyPressed(KeyEvent event) {
        KeyCode code = event.getCode();
        if (code == KeyCode.ESCAPE) {
            if (!shell.isModalShowing()) {
                clearKeys();
                MatchMenuPopup.show(shell, controller::leaveRoom);
                event.consume();
            }
            return;
        }
        if (code == KeyCode.F3) {
            if (heldKeys.add(code)) {
                diagnostics.setVisible(!diagnostics.isVisible());
                diagnostics.setManaged(diagnostics.isVisible());
            }
            event.consume();
            return;
        }
        if (!heldKeys.add(code)) {
            return;
        }
        Direction direction = direction(code);
        if (direction != null) {
            input.pressDirection(direction, System.nanoTime());
            event.consume();
        } else if (code == KeyCode.SPACE) {
            input.pressBomb();
            event.consume();
        }
    }

    private void keyReleased(KeyEvent event) {
        KeyCode code = event.getCode();
        heldKeys.remove(code);
        Direction direction = direction(code);
        if (direction != null && heldKeys.stream().noneMatch(key -> direction(key) == direction)) {
            input.releaseDirection(direction, System.nanoTime());
            event.consume();
        } else if (code == KeyCode.SPACE) {
            input.releaseBomb();
            event.consume();
        }
    }

    private void clearKeys() {
        heldKeys.clear();
        input.reset();
    }

    private void updateDiagnostics(long nowNanos) {
        fpsFrames++;
        if (fpsWindowNanos == 0) fpsWindowNanos = nowNanos;
        long elapsed = nowNanos - fpsWindowNanos;
        if (elapsed >= 500_000_000L) {
            displayedFps = fpsFrames * 1_000_000_000.0 / elapsed;
            fpsFrames = 0;
            fpsWindowNanos = nowNanos;
            diagnostics.setText("FPS %.0f  ·  %.2f ms".formatted(displayedFps, renderer.lastRenderMillis()));
        }
    }

    private static Direction direction(KeyCode code) {
        return switch (code) {
            case W, UP -> Direction.UP;
            case S, DOWN -> Direction.DOWN;
            case A, LEFT -> Direction.LEFT;
            case D, RIGHT -> Direction.RIGHT;
            default -> null;
        };
    }
}
