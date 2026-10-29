package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.SvgAssets;
import javafx.scene.layout.Pane;

/** Home menu button (mockup 1): large blue tile whose illustrated icon spills over its left edge. */
public final class MenuTileButton extends GameButton {

    private static final double ICON_SIZE = 78;
    private static final double SLOT_SIZE = 44;

    public MenuTileButton(String text, SvgAssets assets, String iconAsset) {
        super(text, Tone.BLUE, Size.L, OutlinedText.Style.MENU);
        getStyleClass().add("menu-tile");
        // The icon is larger than the button, so it hangs from a fixed-size slot without growing the layout.
        SvgView icon = new SvgView(assets, iconAsset, ICON_SIZE, ICON_SIZE);
        icon.setManaged(false);
        icon.relocate(-30, -22);
        Pane slot = new Pane(icon);
        slot.setMinSize(SLOT_SIZE, SLOT_SIZE);
        slot.setPrefSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMaxSize(SLOT_SIZE, SLOT_SIZE);
        slot.setMouseTransparent(true);
        setIcon(slot);
    }
}
