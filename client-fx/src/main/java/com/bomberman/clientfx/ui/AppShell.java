package com.bomberman.clientfx.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.util.Duration;

/**
 * Window content: a fixed 1280×720 design frame scaled uniformly to fit the window and
 * centred on the outer background, with the current screen below a toast layer.
 */
public final class AppShell {

    public static final double DESIGN_WIDTH = 1280;
    public static final double DESIGN_HEIGHT = 720;

    private static final Duration TRANSITION = Duration.millis(200);
    private static final Duration TOAST_VISIBLE = Duration.seconds(3);
    private static final int MAX_TOASTS = 2;

    private final Pane viewport = new Pane();
    private final StackPane frame = new StackPane();
    private final StackPane screenLayer = new StackPane();
    private final VBox toastLayer = new VBox(8);
    private final Scale scale = new Scale(1, 1, 0, 0);

    private Screen currentScreen;

    public AppShell() {
        viewport.getStyleClass().add("app-viewport");
        frame.getStyleClass().add("app-frame");
        frame.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        frame.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        frame.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        frame.getTransforms().add(scale);

        toastLayer.setAlignment(Pos.TOP_CENTER);
        toastLayer.setPadding(new Insets(24, 0, 0, 0));
        toastLayer.setMouseTransparent(true);
        toastLayer.setPickOnBounds(false);

        frame.getChildren().addAll(screenLayer, toastLayer);
        viewport.getChildren().add(frame);
        viewport.widthProperty().addListener(observable -> fitFrame());
        viewport.heightProperty().addListener(observable -> fitFrame());
    }

    public Parent root() {
        return viewport;
    }

    public void show(Screen screen) {
        if (screen == currentScreen) {
            return;
        }
        if (currentScreen != null) {
            currentScreen.onHide();
        }
        currentScreen = screen;

        Node content = screen.root();
        screenLayer.getChildren().setAll(content);
        content.setOpacity(0);
        content.setTranslateY(16);
        FadeTransition fade = new FadeTransition(TRANSITION, content);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(TRANSITION, content);
        slide.setToY(0);
        new ParallelTransition(fade, slide).play();

        screen.onShow();
    }

    public void showToast(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        if (toastLayer.getChildren().size() >= MAX_TOASTS) {
            toastLayer.getChildren().removeLast();
        }
        Label toast = new Label(message);
        toast.getStyleClass().add("toast");
        toast.setWrapText(true);
        toast.setMaxWidth(640);
        toastLayer.getChildren().addFirst(toast);

        toast.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), toast);
        fadeIn.setToValue(1);
        FadeTransition fadeOut = new FadeTransition(TRANSITION, toast);
        fadeOut.setToValue(0);
        SequentialTransition lifetime = new SequentialTransition(
                fadeIn,
                new PauseTransition(TOAST_VISIBLE),
                fadeOut
        );
        lifetime.setOnFinished(event -> toastLayer.getChildren().remove(toast));
        lifetime.play();
    }

    private void fitFrame() {
        double width = viewport.getWidth();
        double height = viewport.getHeight();
        double factor = Math.min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT);
        scale.setX(factor);
        scale.setY(factor);
        frame.relocate(
                (width - DESIGN_WIDTH * factor) / 2,
                (height - DESIGN_HEIGHT * factor) / 2
        );
    }
}
