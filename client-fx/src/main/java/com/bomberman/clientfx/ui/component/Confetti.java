package com.bomberman.clientfx.ui.component;

import javafx.animation.AnimationTimer;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Falling confetti for the VICTORY screen. Fill the parent and call {@link #burst()}. */
public final class Confetti extends Pane {

    private static final int PIECES = 80;
    private static final double LIFETIME_SECONDS = 2.5;
    private static final double GRAVITY = 900;
    private static final Color[] COLOURS = {
            Color.web("#FFC93C"), Color.web("#E84B4B"), Color.web("#5AA9FF"),
            Color.web("#6BD968"), Color.web("#8E63E8"), Color.web("#FF8AD8")
    };

    private final List<Piece> pieces = new ArrayList<>();
    private final AnimationTimer timer = new AnimationTimer() {
        private long previous;

        @Override
        public void start() {
            previous = 0;
            super.start();
        }

        @Override
        public void handle(long now) {
            double delta = previous == 0 ? 0 : (now - previous) / 1e9;
            previous = now;
            step(delta);
        }
    };

    public Confetti() {
        setMouseTransparent(true);
        setPickOnBounds(false);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);
    }

    public void burst() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double width = Math.max(getWidth(), 1);
        for (int index = 0; index < PIECES; index++) {
            Rectangle shape = new Rectangle(10, 16, COLOURS[index % COLOURS.length]);
            shape.setArcWidth(4);
            shape.setArcHeight(4);
            shape.setStroke(Color.web("#2A1F3D"));
            shape.setStrokeWidth(1.5);
            shape.relocate(random.nextDouble(width), -30 - random.nextDouble(120));
            getChildren().add(shape);
            pieces.add(new Piece(
                    shape,
                    random.nextDouble(-120, 120),
                    random.nextDouble(-200, 60),
                    random.nextDouble(-360, 360),
                    LIFETIME_SECONDS * random.nextDouble(0.7, 1)
            ));
        }
        timer.start();
    }

    public void stopAndClear() {
        timer.stop();
        pieces.clear();
        getChildren().clear();
    }

    private void step(double delta) {
        pieces.removeIf(piece -> {
            piece.age += delta;
            piece.velocityY += GRAVITY * delta * 0.35;
            piece.shape.setLayoutX(piece.shape.getLayoutX() + piece.velocityX * delta);
            piece.shape.setLayoutY(piece.shape.getLayoutY() + piece.velocityY * delta);
            piece.shape.setRotate(piece.shape.getRotate() + piece.spin * delta);
            piece.shape.setOpacity(Math.max(0, 1 - Math.max(0, piece.age - piece.lifetime + 0.5) / 0.5));
            boolean finished = piece.age >= piece.lifetime;
            if (finished) {
                getChildren().remove(piece.shape);
            }
            return finished;
        });
        if (pieces.isEmpty()) {
            timer.stop();
        }
    }

    private static final class Piece {
        private final Rectangle shape;
        private final double velocityX;
        private final double spin;
        private final double lifetime;
        private double velocityY;
        private double age;

        private Piece(Rectangle shape, double velocityX, double velocityY, double spin, double lifetime) {
            this.shape = shape;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.spin = spin;
            this.lifetime = lifetime;
        }
    }
}
