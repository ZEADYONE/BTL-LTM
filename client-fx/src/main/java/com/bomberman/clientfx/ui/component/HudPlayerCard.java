package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Match HUD card (mockup 5): avatar, name, slot tag, bombs available and blast range. */
public final class HudPlayerCard extends HBox {

    public static final double WIDTH = 222;

    private final PlayerHead avatar;
    private final Label bombs = new Label();
    private final Label range = new Label();

    public HudPlayerCard(SvgAssets assets, int slotIndex, String playerName) {
        getStyleClass().add("hud-card");
        setPrefWidth(WIDTH);
        setMaxWidth(WIDTH);
        avatar = new PlayerHead(assets, TeamColor.forSlot(slotIndex), 54, true);

        Label name = new Label(playerName);
        name.getStyleClass().add("label-name");
        name.setMaxWidth(100);
        HBox nameRow = new HBox(6, name, new Tag("P" + (slotIndex + 1), Tag.Kind.NEUTRAL));
        nameRow.setAlignment(Pos.CENTER_LEFT);

        bombs.getStyleClass().add("hud-stat");
        range.getStyleClass().add("hud-stat");
        HBox stats = new HBox(6,
                new SvgView(assets, AssetIds.BOMB, 24, 24), bombs,
                new SvgView(assets, AssetIds.ICON_FIRE, 24, 24), range);
        stats.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(avatar, new VBox(2, nameRow, stats));
    }

    public void update(int bombsAvailable, int blastRange, boolean alive) {
        bombs.setText(Integer.toString(bombsAvailable));
        range.setText(Integer.toString(blastRange));
        avatar.setState(alive ? PlayerHead.State.NORMAL : PlayerHead.State.OUT);
        getStyleClass().remove("out");
        if (!alive) {
            getStyleClass().add("out");
        }
    }
}
