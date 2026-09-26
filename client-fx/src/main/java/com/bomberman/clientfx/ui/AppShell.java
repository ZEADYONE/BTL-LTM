package com.bomberman.clientfx.ui;

import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.theme.Motion;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Locale;

/**
 * Window content: a fixed 1280×720 design canvas, scaled uniformly to fit the window and
 * centred on the outer background. Framed screens sit inside the orange frame; toasts and
 * modal popups are layered above every screen.
 */
public final class AppShell {

    public static final double DESIGN_WIDTH = 1280;
    public static final double DESIGN_HEIGHT = 720;
    /** Margin between the canvas edge and the orange frame, measured from the mockups. */
    public static final Insets FRAME_MARGIN = new Insets(36, 64, 36, 64);

    private static final Duration TRANSITION = Duration.millis(200);
    private static final Duration SCALE_SETTLE = Duration.millis(150);
    private static final int MAX_TOASTS = 2;
    private static final double PATTERN_TILE = 160;
    private static final PseudoClass BLEED = PseudoClass.getPseudoClass("bleed");

    private final Pane viewport = new Pane();
    private final Region outerPattern = new Region();
    private final StackPane canvas = new StackPane();
    private final Region frameShadow = new Region();
    private final StackPane frame = new StackPane();
    private final Region framePattern = new Region();
    private final StackPane screenLayer = new StackPane();
    private final VBox toastLayer = new VBox(8);
    private final StackPane modalLayer = new StackPane();
    private final Scale scale = new Scale(1, 1, 0, 0);
    private final ReadOnlyDoubleWrapper renderScale = new ReadOnlyDoubleWrapper(this, "renderScale", 1);
    private final PauseTransition renderScaleSettle = new PauseTransition(SCALE_SETTLE);

    private Screen currentScreen;
    private Stage stage;
    private boolean modalDismissible;

    public AppShell() {
        viewport.getStyleClass().add("app-viewport");
        outerPattern.getStyleClass().add("outer-pattern");
        outerPattern.setMouseTransparent(true);
        outerPattern.setManaged(false);

        canvas.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        canvas.setPrefSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        canvas.setMaxSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        canvas.getTransforms().add(scale);

        // The drop shadow lives on a childless node behind the frame: an effect on the frame itself
        // would re-render every screen into an offscreen texture on each animated frame.
        frameShadow.getStyleClass().add("app-frame-shadow");
        frameShadow.setMouseTransparent(true);
        frame.getStyleClass().add("app-frame");
        framePattern.getStyleClass().add("frame-pattern");
        framePattern.setMouseTransparent(true);
        frame.getChildren().addAll(framePattern, screenLayer);

        toastLayer.setAlignment(Pos.TOP_CENTER);
        toastLayer.setPadding(new Insets(56, 0, 0, 0));
        toastLayer.setMouseTransparent(true);
        toastLayer.setPickOnBounds(false);

        modalLayer.getStyleClass().add("modal-overlay");
        modalLayer.setVisible(false);
        modalLayer.setOnMouseClicked(event -> {
            if (event.getTarget() == modalLayer && modalDismissible) {
                closeModal();
            }
        });

        canvas.getChildren().addAll(frameShadow, frame, toastLayer, modalLayer);
        viewport.getChildren().addAll(outerPattern, canvas);
        viewport.widthProperty().addListener(observable -> fitCanvas());
        viewport.heightProperty().addListener(observable -> fitCanvas());
        viewport.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && modalLayer.isVisible() && modalDismissible) {
                closeModal();
                event.consume();
            }
        });
        renderScaleSettle.setOnFinished(event -> renderScale.set(currentRenderScale()));
        setFramed(true);
    }

    public Parent root() {
        return viewport;
    }

    /** Tracks the window's output scale (Windows display scaling) for sharp images. */
    public void attachTo(Stage stage) {
        this.stage = stage;
        stage.outputScaleXProperty().addListener(observable -> scheduleRenderScale());
        scheduleRenderScale();
    }

    /**
     * Device pixels per logical design pixel, updated 150 ms after the window stops changing.
     * Image views bind to it to decide how large to rasterise.
     */
    public ReadOnlyDoubleProperty renderScaleProperty() {
        return renderScale.getReadOnlyProperty();
    }

    /** Uses the rasterised {@code pattern_bomb} tile on the frame and, whitened, on the outer background. */
    public void setBackgroundPattern(Image tile) {
        framePattern.setBackground(tiled(tile));
        outerPattern.setBackground(tiled(whitened(tile)));
    }

    private static Background tiled(Image tile) {
        return new Background(new BackgroundImage(
                tile,
                BackgroundRepeat.REPEAT,
                BackgroundRepeat.REPEAT,
                BackgroundPosition.DEFAULT,
                new BackgroundSize(PATTERN_TILE, PATTERN_TILE, false, false, false, false)
        ));
    }

    /** Same shapes in white, done once per tile instead of a full-window colour effect. */
    private static Image whitened(Image tile) {
        int width = (int) tile.getWidth();
        int height = (int) tile.getHeight();
        PixelReader reader = tile.getPixelReader();
        WritableImage white = new WritableImage(width, height);
        PixelWriter writer = white.getPixelWriter();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setArgb(x, y, (reader.getArgb(x, y) & 0xFF000000) | 0x00FFFFFF);
            }
        }
        return white;
    }

    public void show(Screen screen) {
        if (screen == currentScreen) {
            return;
        }
        if (currentScreen != null) {
            currentScreen.onHide();
        }
        currentScreen = screen;
        setFramed(screen.framed());

        Node content = screen.root();
        screenLayer.getChildren().setAll(content);
        content.setOpacity(0);
        content.setTranslateY(16);
        FadeTransition fade = new FadeTransition(TRANSITION, content);
        fade.setToValue(1);
        TranslateTransition slide = new TranslateTransition(TRANSITION, content);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, slide).play();

        screen.onShow();
    }

    public void showToast(Feedback feedback) {
        if (feedback.message().isBlank()) {
            return;
        }
        if (toastLayer.getChildren().size() >= MAX_TOASTS) {
            toastLayer.getChildren().removeLast();
        }
        Label toast = new Label(feedback.message());
        toast.getStyleClass().addAll("toast", "toast-" + feedback.kind().name().toLowerCase(Locale.ROOT));
        toast.setWrapText(true);
        toast.setMaxWidth(640);
        toastLayer.getChildren().addFirst(toast);

        toast.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(150), toast);
        fadeIn.setToValue(1);
        FadeTransition fadeOut = new FadeTransition(TRANSITION, toast);
        fadeOut.setToValue(0);
        Duration visible = Duration.seconds(feedback.kind() == Feedback.Kind.ERROR ? 4 : 3);
        SequentialTransition lifetime = new SequentialTransition(fadeIn, new PauseTransition(visible), fadeOut);
        lifetime.setOnFinished(event -> toastLayer.getChildren().remove(toast));
        lifetime.play();
    }

    /**
     * Shows {@code content} centred over a dimmed canvas.
     * @param dismissible whether ESC or a click outside the content closes it
     */
    public void showModal(Node content, boolean dismissible) {
        modalDismissible = dismissible;
        modalLayer.getChildren().setAll(content);
        modalLayer.setVisible(true);
        content.setOpacity(0);
        content.setScaleX(0.9);
        content.setScaleY(0.9);
        FadeTransition fade = new FadeTransition(Duration.millis(180), content);
        fade.setToValue(1);
        ScaleTransition grow = new ScaleTransition(Duration.millis(180), content);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(Motion.BACK_OUT);
        new ParallelTransition(fade, grow).play();
    }

    public void closeModal() {
        modalLayer.setVisible(false);
        modalLayer.getChildren().clear();
    }

    private void setFramed(boolean framed) {
        StackPane.setMargin(frame, framed ? FRAME_MARGIN : Insets.EMPTY);
        StackPane.setMargin(frameShadow, FRAME_MARGIN);
        frameShadow.setVisible(framed);
        frame.pseudoClassStateChanged(BLEED, !framed);
    }

    private void fitCanvas() {
        double width = viewport.getWidth();
        double height = viewport.getHeight();
        outerPattern.resizeRelocate(0, 0, width, height);
        double factor = Math.min(width / DESIGN_WIDTH, height / DESIGN_HEIGHT);
        scale.setX(factor);
        scale.setY(factor);
        canvas.relocate((width - DESIGN_WIDTH * factor) / 2, (height - DESIGN_HEIGHT * factor) / 2);
        scheduleRenderScale();
    }

    private void scheduleRenderScale() {
        renderScaleSettle.playFromStart();
    }

    private double currentRenderScale() {
        double outputScale = stage == null ? 1 : stage.getOutputScaleX();
        return Math.max(0.25, scale.getX() * outputScale);
    }
}
