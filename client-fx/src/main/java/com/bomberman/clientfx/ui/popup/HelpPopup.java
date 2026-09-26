package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.component.GameButton;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** How to play (docs/ui-redesign/02, section 4.3). Numbers follow the server rules in ServerRules / 06. */
public final class HelpPopup {

    private static final List<String> RULES = List.of(
            "2–4 players per room. Everyone starts with 1 bomb and a blast range of 2 tiles.",
            "Bombs explode after 3 seconds.",
            "A blast stops at stone blocks and breaks the first crate it reaches.",
            "A blast sets off any other bomb it reaches.",
            "Players and bombs block the way. You can step off a bomb you just placed.",
            "Anyone standing in a blast when it goes off is knocked out.",
            "Last player standing wins. If the last players go down together, it's a draw."
    );

    private HelpPopup() {
    }

    public static void show(AppShell shell) {
        GridPane controls = new GridPane();
        controls.setHgap(14);
        controls.setVgap(6);
        addControl(controls, 0, "Move (hold to keep moving)", "W", "A", "S", "D", "/", "←", "↑", "↓", "→");
        addControl(controls, 1, "Place a bomb", "SPACE");
        addControl(controls, 2, "Match menu", "ESC");
        addControl(controls, 3, "Fullscreen", "F11");

        VBox rules = new VBox(2);
        RULES.forEach(rule -> rules.getChildren().add(Popups.text("•  " + rule)));

        Label scoring = Popups.text("Win +1   ·   Draw +0.5   ·   Loss 0");
        VBox body = new VBox(6,
                heading("CONTROLS"), controls,
                heading("RULES"), rules,
                heading("SCORING"), scoring);

        GameButton close = new GameButton("GOT IT", GameButton.Tone.YELLOW, GameButton.Size.M);
        close.setOnAction(event -> shell.closeModal());
        close.setDefaultButton(true);
        var panel = Popups.frame("HOW TO PLAY", 780, body, close);
        panel.body().setSpacing(10);
        shell.showModal(panel, true);
    }

    private static void addControl(GridPane grid, int row, String action, String... keys) {
        HBox caps = new HBox(4);
        caps.setAlignment(Pos.CENTER_LEFT);
        for (String key : keys) {
            Label cap = new Label(key);
            cap.getStyleClass().add("/".equals(key) ? "key-separator" : "keycap");
            caps.getChildren().add(cap);
        }
        grid.add(caps, 0, row);
        grid.add(Popups.text(action), 1, row);
    }

    private static Node heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("popup-heading");
        return label;
    }
}
