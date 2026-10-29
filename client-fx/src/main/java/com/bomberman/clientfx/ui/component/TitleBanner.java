package com.bomberman.clientfx.ui.component;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** Screen title: a purple block (ROOM LOBBY) or free text flanked by yellow dashes (SELECT MODE). */
public final class TitleBanner extends HBox {

    private TitleBanner() {
    }

    public static TitleBanner purple(String title, Node icon) {
        TitleBanner banner = new TitleBanner();
        banner.getStyleClass().add("title-banner");
        if (icon != null) {
            banner.getChildren().add(icon);
        }
        banner.getChildren().add(new OutlinedText(title, OutlinedText.Style.HEADING));
        banner.setMaxWidth(USE_PREF_SIZE);
        return banner;
    }

    public static TitleBanner burst(String title) {
        TitleBanner banner = new TitleBanner();
        banner.setAlignment(Pos.CENTER);
        banner.setSpacing(18);
        banner.getChildren().addAll(dashes(-1), new OutlinedText(title, OutlinedText.Style.TITLE), dashes(1));
        banner.setMaxWidth(USE_PREF_SIZE);
        return banner;
    }

    /** Three short strokes fanning away from the title; {@code side} is -1 (left) or 1 (right). */
    private static Node dashes(int side) {
        Rectangle middle = dash(0);
        middle.setTranslateX(side * 6);
        VBox column = new VBox(10, dash(side * 35), middle, dash(side * -35));
        column.setAlignment(Pos.CENTER);
        return column;
    }

    private static Rectangle dash(double angle) {
        Rectangle dash = new Rectangle(26, 10);
        dash.getStyleClass().add("banner-dash");
        dash.setRotate(angle);
        return dash;
    }
}
