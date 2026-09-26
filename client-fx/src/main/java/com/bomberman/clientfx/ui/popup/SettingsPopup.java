package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.StatusDot;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/** Settings from Home: server (read-only), fullscreen, log out. No audio settings: the game has no sound. */
public final class SettingsPopup {

    private SettingsPopup() {
    }

    public static void show(AppShell shell, Stage stage, ClientState state,
                            GameClientController controller, Navigator navigator) {
        Label address = Popups.text(controller.networkConfig().displayAddress());
        HBox server = new HBox(10, new StatusDot(state.isConnected()), address);
        server.setAlignment(Pos.CENTER_LEFT);

        GameButton fullscreen = new GameButton(fullscreenLabel(stage), GameButton.Tone.PURPLE, GameButton.Size.S);
        fullscreen.setOnAction(event -> {
            stage.setFullScreen(!stage.isFullScreen());
            shell.closeModal();
            show(shell, stage, state, controller, navigator);
        });

        GameButton logout = new GameButton("LOG OUT", GameButton.Tone.RED, GameButton.Size.S);
        logout.setOnAction(event -> Popups.confirm(shell, "LOG OUT", "Log out of " + state.getCurrentUsername() + "?",
                "LOG OUT", () -> {
                    controller.logout();
                    navigator.show(ScreenId.LOGIN);
                }));

        GridPane rows = new GridPane();
        rows.setHgap(24);
        rows.setVgap(16);
        addRow(rows, 0, "SERVER", server);
        addRow(rows, 1, "DISPLAY", fullscreen);
        addRow(rows, 2, "ACCOUNT", logout);

        GameButton close = new GameButton("CLOSE", GameButton.Tone.BLUE, GameButton.Size.M);
        close.setOnAction(event -> shell.closeModal());
        shell.showModal(Popups.frame("SETTINGS", 520, rows, close), true);
    }

    private static String fullscreenLabel(Stage stage) {
        return stage.isFullScreen() ? "FULLSCREEN: ON" : "FULLSCREEN: OFF";
    }

    private static void addRow(GridPane grid, int row, String name, Node value) {
        Label label = new Label(name);
        label.getStyleClass().add("popup-heading");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }
}
