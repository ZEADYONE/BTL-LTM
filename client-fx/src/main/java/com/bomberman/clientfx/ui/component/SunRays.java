package com.bomberman.clientfx.ui.component;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

/** Soft rotating light rays behind a hero character (Home, Result). */
public final class SunRays extends Group {

    private static final int RAYS = 12;
    private static final double HALF_ANGLE = Math.toRadians(8);

    private final RotateTransition rotation = new RotateTransition(Duration.seconds(20), this);

    public SunRays(double radius) {
        getChildren().add(new Circle(radius * 0.45, Color.rgb(255, 245, 200, 0.35)));
        for (int ray = 0; ray < RAYS; ray++) {
            double angle = 2 * Math.PI * ray / RAYS;
            Polygon wedge = new Polygon(
                    0, 0,
                    radius * Math.cos(angle - HALF_ANGLE), radius * Math.sin(angle - HALF_ANGLE),
                    radius * Math.cos(angle + HALF_ANGLE), radius * Math.sin(angle + HALF_ANGLE)
            );
            wedge.setFill(Color.rgb(255, 245, 200, 0.18));
            getChildren().add(wedge);
        }
        setMouseTransparent(true);
        rotation.setByAngle(360);
        rotation.setCycleCount(Animation.INDEFINITE);
        rotation.setInterpolator(Interpolator.LINEAR);
    }

    public void play() {
        rotation.play();
    }

    public void stop() {
        rotation.stop();
    }
}
