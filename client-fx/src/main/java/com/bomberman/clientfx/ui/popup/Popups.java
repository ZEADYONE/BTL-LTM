package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.Panel;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Shared popup frame (cream panel with a dark header, mockup 3) and the simple confirm / message popups. */
public final class Popups {

    private Popups() {
    }

    /** Cream panel with {@code title}, {@code body} and a centred row of {@code buttons}. */
    public static Panel frame(String title, double width, Node body, Node... buttons) {
        HBox actions = new HBox(12, buttons);
        actions.setAlignment(Pos.CENTER);
        Panel panel = new Panel(Panel.Style.CREAM, title, body, actions);
        panel.body().setSpacing(18);
        panel.setPrefWidth(width);
        panel.setMaxSize(width, Panel.USE_PREF_SIZE);
        return panel;
    }

    public static Label text(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("label-body");
        label.setWrapText(true);
        // Without this a wrapping label keeps a one-line height inside a VBox and truncates with "…".
        label.setMinHeight(Label.USE_PREF_SIZE);
        return label;
    }

    /** Asks before a destructive action (leave room, leave match, log out). */
    public static void confirm(AppShell shell, String title, String question, String confirmText, Runnable onConfirm) {
        GameButton cancel = new GameButton("CANCEL", GameButton.Tone.BLUE, GameButton.Size.M);
        cancel.setOnAction(event -> shell.closeModal());
        GameButton confirm = new GameButton(confirmText, GameButton.Tone.RED, GameButton.Size.M);
        confirm.setOnAction(event -> {
            shell.closeModal();
            onConfirm.run();
        });
        shell.showModal(frame(title, 440, text(question), cancel, confirm), true);
        cancel.requestFocus();
    }

    /** Lost connection; not dismissible with ESC, the player must acknowledge it. */
    public static void connectionLost(AppShell shell, String address) {
        GameButton ok = new GameButton("OK", GameButton.Tone.YELLOW, GameButton.Size.M);
        ok.setOnAction(event -> shell.closeModal());
        ok.setDefaultButton(true);
        shell.showModal(frame("CONNECTION LOST", 460, text("Lost connection to " + address + "."), ok), false);
        ok.requestFocus();
    }
}
