package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.component.GameButton;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;

/** ESC menu shown over a live match; opening it never pauses the authoritative server. */
public final class MatchMenuPopup {

    private MatchMenuPopup() {
    }

    public static void show(AppShell shell, Runnable leaveMatch) {
        GameButton resume = new GameButton("RESUME", GameButton.Tone.GREEN, GameButton.Size.M);
        resume.setMaxWidth(Double.MAX_VALUE);
        resume.setOnAction(event -> shell.closeModal());
        GameButton help = new GameButton("HELP", GameButton.Tone.BLUE, GameButton.Size.M);
        help.setMaxWidth(Double.MAX_VALUE);
        help.setOnAction(event -> HelpPopup.show(shell));
        GameButton leave = new GameButton("LEAVE MATCH", GameButton.Tone.RED, GameButton.Size.M);
        leave.setMaxWidth(Double.MAX_VALUE);
        leave.setOnAction(event -> Popups.confirm(
                shell,
                "LEAVE MATCH",
                "Leave the match? You will be knocked out and lose.",
                "LEAVE",
                leaveMatch
        ));
        VBox actions = new VBox(10, Popups.text("The match keeps running."), resume, help, leave);
        actions.setAlignment(Pos.CENTER);
        shell.showModal(Popups.frame("MATCH MENU", 440, actions), true);
        resume.requestFocus();
    }
}
