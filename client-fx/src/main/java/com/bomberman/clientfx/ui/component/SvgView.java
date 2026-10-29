package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 * Displays an asset at a fixed logical size. The bitmap is rendered at the current
 * {@link SvgAssets#renderScaleProperty() render scale}, so it stays sharp when the window
 * is resized or moved to a high-DPI screen.
 */
public final class SvgView extends Region {

    private final SvgAssets assets;
    private final String asset;
    private final double logicalWidth;
    private final double logicalHeight;
    private final ImageView imageView = new ImageView();
    private final InvalidationListener scaleListener = observable -> refresh();
    private TeamColor team;
    private long requestToken;

    public SvgView(SvgAssets assets, String asset, double width, double height) {
        this(assets, asset, null, width, height);
    }

    public SvgView(SvgAssets assets, String asset, TeamColor team, double width, double height) {
        this.assets = assets;
        this.asset = asset;
        this.team = team;
        this.logicalWidth = width;
        this.logicalHeight = height;
        getStyleClass().add("svg-view");
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setSmooth(true);
        getChildren().add(imageView);
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        assets.renderScaleProperty().addListener(new WeakInvalidationListener(scaleListener));
        refresh();
    }

    public void setTeam(TeamColor team) {
        if (this.team != team) {
            this.team = team;
            refresh();
        }
    }

    public TeamColor getTeam() {
        return team;
    }

    private void refresh() {
        // Quarter steps keep window resizing from re-rendering on every pixel.
        double scale = Math.max(0.25, Math.ceil(assets.renderScaleProperty().get() * 4) / 4);
        long token = ++requestToken;
        assets.request(
                asset,
                team,
                (int) Math.ceil(logicalWidth * scale),
                (int) Math.ceil(logicalHeight * scale)
        ).thenAccept(image -> Platform.runLater(() -> {
            if (token == requestToken) {
                imageView.setImage(image);
            }
        }));
    }
}
