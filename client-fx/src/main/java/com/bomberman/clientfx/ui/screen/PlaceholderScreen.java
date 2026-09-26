package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.ScreenId;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.OutlinedText;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Stands in for screens that later phases will build, with a way back out. */
public final class PlaceholderScreen implements Screen {

    private final VBox root = new VBox(24);

    public PlaceholderScreen(ScreenId id, String actionText, Runnable action) {
        GameButton button = new GameButton(actionText, GameButton.Tone.BLUE, GameButton.Size.L);
        button.setOnAction(event -> action.run());
        root.getChildren().addAll(
                new OutlinedText(id.name().replace('_', ' '), OutlinedText.Style.TITLE),
                new OutlinedText("COMING IN A LATER PHASE", OutlinedText.Style.HEADING),
                button
        );
        root.setAlignment(Pos.CENTER);
    }

    @Override
    public Node root() {
        return root;
    }
}
