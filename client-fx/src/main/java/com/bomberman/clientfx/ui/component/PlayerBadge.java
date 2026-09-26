package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Top-left player badge of the Home mockup: avatar, name and a subtitle line (rank and points). */
public final class PlayerBadge extends HBox {

    private final PlayerHead avatar;
    private final Label name = new Label();
    private final Label subtitle = new Label();

    public PlayerBadge(SvgAssets assets) {
        getStyleClass().add("player-badge");
        avatar = new PlayerHead(assets, TeamColor.RED, 60, true);
        name.getStyleClass().add("label-name");
        subtitle.getStyleClass().add("badge-subtitle");
        getChildren().addAll(avatar, new VBox(2, name, subtitle));
        setMaxWidth(USE_PREF_SIZE);
    }

    public void setPlayer(String playerName, TeamColor team, String subtitleText) {
        name.setText(playerName);
        avatar.setTeam(team);
        subtitle.setText(subtitleText);
    }
}
