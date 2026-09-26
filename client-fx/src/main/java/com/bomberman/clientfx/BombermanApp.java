package com.bomberman.clientfx;

import com.bomberman.clientfx.network.ClientMessageDispatcher;
import com.bomberman.clientfx.network.ClientNetworkConfig;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.network.GameNetworkClient;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.ScreenNavigator;
import com.bomberman.clientfx.ui.screen.HomeScreen;
import com.bomberman.clientfx.ui.screen.LoginScreen;
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

/** Wires state, networking and screens, then opens the window. */
public final class BombermanApp extends Application {

    private static final String TITLE = "Bomberman Online Mini";
    private static final double MIN_WIDTH = 960;
    private static final double MIN_HEIGHT = 540;

    private GameNetworkClient networkClient;
    private GameClientController controller;

    @Override
    public void start(Stage stage) {
        UserPreferences preferences = UserPreferences.forCurrentUser();
        ClientState state = new ClientState();
        AppShell shell = new AppShell();
        ScreenNavigator navigator = new ScreenNavigator(shell);

        networkClient = new GameNetworkClient(
                new ClientMessageDispatcher(state, navigator, Platform::runLater)
        );
        controller = new GameClientController(
                networkClient,
                state,
                ClientNetworkConfig.load(preferences.savedServer().orElse(null)),
                Platform::runLater
        );
        state.addFeedbackListener(shell::showToast);

        navigator.register(ScreenId.LOGIN, new LoginScreen(state, controller, preferences));
        navigator.register(ScreenId.HOME, new HomeScreen(state, controller, navigator));

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = Math.min(AppShell.DESIGN_WIDTH, screenBounds.getWidth() * 0.9);
        double height = Math.min(width * 9 / 16, screenBounds.getHeight() * 0.9);
        Scene scene = new Scene(shell.root(), width, height);
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
        // ESC is reserved for the in-match menu, so only F11 leaves fullscreen.
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.setFullScreenExitHint("");
        stage.fullScreenProperty().addListener(
                (observable, wasFullScreen, isFullScreen) -> preferences.setFullscreen(isFullScreen)
        );
        stage.setScene(scene);
        stage.setFullScreen(preferences.fullscreen());

        navigator.show(ScreenId.LOGIN);
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
    }
}
