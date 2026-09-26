package com.bomberman.clientfx.ui.component;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/** Small outlined dot: green when connected / online, grey otherwise. */
public final class StatusDot extends Circle {

    private static final Color ON = Color.web("#43C463");
    private static final Color OFF = Color.web("#8C8FA3");

    public StatusDot(boolean on) {
        super(7);
        setStroke(Color.web("#2A1F3D"));
        setStrokeWidth(2);
        setOn(on);
    }

    public void setOn(boolean on) {
        setFill(on ? ON : OFF);
    }
}
