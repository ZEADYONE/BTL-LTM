package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.audio.AudioManager;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.Navigator;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.Panel;
import com.bomberman.clientfx.ui.component.StatusDot;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.function.DoubleConsumer;

/** Settings from Home: audio, server (read-only), fullscreen and account actions. */
public final class SettingsPopup {

    private static final double SLIDER_WIDTH = 230;
    private static final double SLIDER_HEIGHT = 32;
    private static final double VOLUME_CONTROL_WIDTH = 296;

    private SettingsPopup() {
    }

    public static void show(AppShell shell, Stage stage, ClientState state,
                            GameClientController controller, Navigator navigator,
                            AudioManager audioManager) {
        Label address = Popups.text(controller.networkConfig().displayAddress());
        HBox server = new HBox(10, new StatusDot(state.isConnected()), address);
        server.setAlignment(Pos.CENTER_LEFT);

        GameButton fullscreen = new GameButton(fullscreenLabel(stage), GameButton.Tone.PURPLE, GameButton.Size.S);
        fullscreen.setOnAction(event -> {
            stage.setFullScreen(!stage.isFullScreen());
            shell.closeModal();
            show(shell, stage, state, controller, navigator, audioManager);
        });

        HBox masterVolume = volumeControl(audioManager.masterVolume(), audioManager::setMasterVolume);
        HBox musicVolume = volumeControl(audioManager.musicVolume(), audioManager::setMusicVolume);
        HBox sfxVolume = volumeControl(audioManager.sfxVolume(), audioManager::setSfxVolume);
        GameButton mute = new GameButton(muteLabel(audioManager), GameButton.Tone.PURPLE, GameButton.Size.S);
        mute.setOnAction(event -> {
            audioManager.setMuted(!audioManager.muted());
            mute.setLabelText(muteLabel(audioManager));
        });

        GameButton logout = new GameButton("LOG OUT", GameButton.Tone.RED, GameButton.Size.S);
        logout.setOnAction(event -> Popups.confirm(shell, "LOG OUT", "Log out of " + state.getCurrentUsername() + "?",
                "LOG OUT", () -> {
                    controller.logout();
                    navigator.show(ScreenId.LOGIN);
                }));

        GridPane rows = new GridPane();
        rows.setHgap(24);
        rows.setVgap(8);
        addRow(rows, 0, "SERVER", server);
        addRow(rows, 1, "DISPLAY", fullscreen);
        addRow(rows, 2, "MASTER", masterVolume);
        addRow(rows, 3, "MUSIC", musicVolume);
        addRow(rows, 4, "SFX", sfxVolume);
        addRow(rows, 5, "MUTE", mute);
        addRow(rows, 6, "ACCOUNT", logout);

        GameButton close = new GameButton("CLOSE", GameButton.Tone.BLUE, GameButton.Size.M);
        close.setOnAction(event -> shell.closeModal());
        Panel panel = Popups.frame("SETTINGS", 620, rows, close);
        panel.getStyleClass().add("settings-popup");
        shell.showModal(panel, true);
    }

    private static String fullscreenLabel(Stage stage) {
        return stage.isFullScreen() ? "FULLSCREEN: ON" : "FULLSCREEN: OFF";
    }

    private static String muteLabel(AudioManager audioManager) {
        return audioManager.muted() ? "MUTED: ON" : "MUTED: OFF";
    }

    private static HBox volumeControl(double initialValue, DoubleConsumer onChange) {
        Slider slider = new Slider(0, 100, initialValue * 100);
        slider.getStyleClass().add("audio-slider");
        slider.setMinSize(SLIDER_WIDTH, SLIDER_HEIGHT);
        slider.setPrefSize(SLIDER_WIDTH, SLIDER_HEIGHT);
        slider.setMaxSize(SLIDER_WIDTH, SLIDER_HEIGHT);
        slider.setBlockIncrement(5);
        // SliderSkin can paint its track outside its layout bounds under the scaled design canvas.
        // An explicit clip keeps the skin inside the fixed control rectangle on every window ratio.
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(slider.widthProperty());
        clip.heightProperty().bind(slider.heightProperty());
        slider.setClip(clip);

        Label percentage = new Label(percentage(slider.getValue()));
        percentage.getStyleClass().add("audio-volume-value");
        percentage.setMinWidth(54);
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            percentage.setText(percentage(newValue.doubleValue()));
            onChange.accept(newValue.doubleValue() / 100.0);
        });

        HBox control = new HBox(12, slider, percentage);
        control.getStyleClass().add("audio-control");
        control.setAlignment(Pos.CENTER_LEFT);
        control.setMinWidth(VOLUME_CONTROL_WIDTH);
        control.setPrefWidth(VOLUME_CONTROL_WIDTH);
        control.setMaxWidth(VOLUME_CONTROL_WIDTH);
        return control;
    }

    private static String percentage(double value) {
        return Math.round(value) + "%";
    }

    private static void addRow(GridPane grid, int row, String name, Node value) {
        Label label = new Label(name);
        label.getStyleClass().add("popup-heading");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }
}
