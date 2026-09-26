package com.bomberman.clientfx;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.asset.SvgRasterizer;
import com.bomberman.clientfx.network.ClientMessageDispatcher;
import com.bomberman.clientfx.network.ClientNetworkConfig;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.network.GameNetworkClient;
import com.bomberman.clientfx.network.PendingRequests;
import com.bomberman.clientfx.game.MatchTracker;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.ScreenNavigator;
import com.bomberman.clientfx.ui.screen.GalleryScreen;
import com.bomberman.clientfx.ui.screen.GameScreen;
import com.bomberman.clientfx.ui.screen.HomeScreen;
import com.bomberman.clientfx.ui.screen.HistoryScreen;
import com.bomberman.clientfx.ui.screen.LeaderboardScreen;
import com.bomberman.clientfx.ui.screen.LoginScreen;
import com.bomberman.clientfx.ui.screen.PlaceholderScreen;
import com.bomberman.clientfx.ui.screen.RoomBrowserScreen;
import com.bomberman.clientfx.ui.screen.RoomLobbyScreen;
import com.bomberman.clientfx.ui.screen.ResultScreen;
import com.bomberman.clientfx.ui.popup.Popups;
import com.bomberman.clientfx.ui.theme.Fonts;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.Objects;

/** Wires state, networking, assets and screens, then opens the window. */
public final class BombermanApp extends Application {

    private static final String TITLE = "Bomberman Online Mini";
    private static final String GALLERY_FLAG = "--gallery";
    private static final double MIN_WIDTH = 960;
    private static final double MIN_HEIGHT = 540;
    private static final int PATTERN_TILE = 160;

    private GameNetworkClient networkClient;
    private GameClientController controller;
    private PendingRequests pendingRequests;
    private SvgAssets assets;

    @Override
    public void start(Stage stage) {
        Fonts.load();
        UserPreferences preferences = UserPreferences.forCurrentUser();
        ClientState state = new ClientState();
        AppShell shell = new AppShell();
        ScreenNavigator navigator = new ScreenNavigator(shell);
        MatchTracker matchTracker = new MatchTracker();
        assets = new SvgAssets(SvgRasterizer.fromClasspath());
        assets.renderScaleProperty().bind(shell.renderScaleProperty());
        shell.renderScaleProperty().addListener(observable -> loadBackgroundPattern(shell));
        loadBackgroundPattern(shell);

        pendingRequests = new PendingRequests(Platform::runLater, PendingRequests.DEFAULT_TIMEOUT,
                () -> state.setFeedback(Feedback.error("Server is not responding.")));
        networkClient = new GameNetworkClient(new ClientMessageDispatcher(
                state, navigator, Platform::runLater, pendingRequests,
                () -> Popups.connectionLost(shell, controller.networkConfig().displayAddress())
        ));
        controller = new GameClientController(
                networkClient,
                state,
                ClientNetworkConfig.load(preferences.savedServer().orElse(null)),
                pendingRequests,
                Platform::runLater
        );
        state.addFeedbackListener(shell::showToast);

        navigator.register(ScreenId.LOGIN, new LoginScreen(state, controller, preferences, assets, shell));
        navigator.register(ScreenId.HOME, new HomeScreen(state, controller, navigator, assets, shell, stage));
        navigator.register(ScreenId.ROOM_BROWSER, new RoomBrowserScreen(state, controller, navigator, assets));
        navigator.register(ScreenId.ROOM_LOBBY, new RoomLobbyScreen(state, controller, assets, shell));
        navigator.register(ScreenId.GAME, new GameScreen(state, controller, navigator, assets, shell, matchTracker));
        navigator.register(ScreenId.RESULT, new ResultScreen(state, controller, navigator, assets, matchTracker));
        navigator.register(ScreenId.LEADERBOARD, new LeaderboardScreen(state, controller, navigator, assets));
        navigator.register(ScreenId.HISTORY, new HistoryScreen(state, controller, navigator, assets));
        navigator.setFallback(id -> switch (id) {
            case RESULT -> new PlaceholderScreen(id, "LEAVE ROOM", controller::leaveRoom);
            default -> new PlaceholderScreen(id, "BACK", () -> navigator.show(ScreenId.HOME));
        });

        Scene scene = new Scene(shell.root(), initialWidth(), initialWidth() * 9 / 16);
        scene.getStylesheets().add(Objects.requireNonNull(
                BombermanApp.class.getResource("/css/game-theme.css"),
                "Missing /css/game-theme.css"
        ).toExternalForm());
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                event.consume();
            }
        });

        stage.setTitle(TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        // ESC is reserved for popups and the in-match menu, so only F11 leaves fullscreen.
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("");
        stage.fullScreenProperty().addListener(
                (observable, wasFullScreen, isFullScreen) -> preferences.setFullscreen(isFullScreen)
        );
        stage.setScene(scene);
        stage.setFullScreen(preferences.fullscreen());
        shell.attachTo(stage);

        if (getParameters().getRaw().contains(GALLERY_FLAG)) {
            shell.show(new GalleryScreen(assets, shell));
        } else {
            navigator.show(ScreenId.LOGIN);
        }
        stage.show();
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.close();
        }
        if (networkClient != null) {
            networkClient.close();
        }
        if (pendingRequests != null) {
            pendingRequests.close();
        }
        if (assets != null) {
            assets.close();
        }
    }

    private void loadBackgroundPattern(AppShell shell) {
        int pixels = (int) Math.ceil(PATTERN_TILE * shell.renderScaleProperty().get());
        assets.request(AssetIds.PATTERN_BOMB, null, pixels, pixels)
                .thenAccept(tile -> Platform.runLater(() -> shell.setBackgroundPattern(tile)));
    }

    private static double initialWidth() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return Math.min(
                Math.min(AppShell.DESIGN_WIDTH, screenBounds.getWidth() * 0.9),
                screenBounds.getHeight() * 0.9 * 16 / 9
        );
    }
}
