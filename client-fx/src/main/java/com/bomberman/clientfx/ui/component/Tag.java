package com.bomberman.clientfx.ui.component;

import javafx.scene.Node;
import javafx.scene.control.Label;

import java.util.Locale;

/** Small outlined badge such as HOST, YOU, WAITING or WINNER. */
public final class Tag extends Label {

    public enum Kind {
        HOST,
        YOU,
        WAITING,
        PLAYING,
        FINISHED,
        WINNER,
        DRAW,
        NEUTRAL
    }

    public Tag(String text, Kind kind) {
        this(text, kind, null);
    }

    public Tag(String text, Kind kind, Node icon) {
        super(text, icon);
        getStyleClass().add("tag");
        setKind(kind);
        setMinWidth(USE_PREF_SIZE);
    }

    public void setKind(Kind kind) {
        getStyleClass().removeIf(styleClass -> styleClass.startsWith("tag-"));
        if (kind != Kind.NEUTRAL) {
            getStyleClass().add("tag-" + kind.name().toLowerCase(Locale.ROOT));
        }
    }
}
