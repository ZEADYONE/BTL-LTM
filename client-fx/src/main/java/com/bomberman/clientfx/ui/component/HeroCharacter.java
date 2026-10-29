package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.util.Duration;

/**
 * Large idle character of the Home and Login screens: light rays, a floor shadow and a gentle bob.
 * Keep it outside scroll panes; its animations would otherwise repaint the whole viewport.
 */
public final class HeroCharacter extends StackPane {

    private static final Duration BOB_HALF_CYCLE = Duration.millis(800);

    private final SvgView character;
    private final SunRays rays;
    private final ParallelTransition idle;

    public HeroCharacter(SvgAssets assets, TeamColor team, double size) {
        rays = new SunRays(size * 0.62);
        Ellipse shadow = new Ellipse(size * 0.26, size * 0.045);
        shadow.setFill(Color.rgb(42, 31, 61, 0.28));
        character = new SvgView(assets, AssetIds.BOMBER_FULL, team, size, size);

        StackPane.setAlignment(shadow, Pos.BOTTOM_CENTER);
        shadow.setTranslateY(-size * 0.04);
        getChildren().addAll(rays, shadow, character);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        setMouseTransparent(true);

        TranslateTransition bob = new TranslateTransition(BOB_HALF_CYCLE, character);
        bob.setByY(-8);
        ScaleTransition squash = new ScaleTransition(BOB_HALF_CYCLE, shadow);
        squash.setToX(0.86);
        squash.setToY(0.86);
        idle = new ParallelTransition(bob, squash);
        idle.setAutoReverse(true);
        idle.setCycleCount(Animation.INDEFINITE);
        idle.setInterpolator(Interpolator.EASE_BOTH);
        bob.setInterpolator(Interpolator.EASE_BOTH);
        squash.setInterpolator(Interpolator.EASE_BOTH);
    }

    public void setTeam(TeamColor team) {
        character.setTeam(team);
    }

    public void play() {
        idle.play();
        rays.play();
    }

    public void stop() {
        idle.pause();
        rays.stop();
    }
}
