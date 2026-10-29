package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.geometry.Pos;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.StackPane;

/** Avatar head in a team colour; optionally inside a rounded coloured box like the mockup badges. */
public final class PlayerHead extends StackPane {

    public enum State {
        NORMAL,
        /** Grey silhouette for an empty slot. */
        EMPTY,
        /** Knocked out: greyed with a skull. */
        OUT
    }

    private final SvgView head;
    private final SvgView skull;
    private final boolean boxed;
    private State state = State.NORMAL;

    public PlayerHead(SvgAssets assets, TeamColor team, double size, boolean boxed) {
        this.boxed = boxed;
        double headSize = boxed ? size * 0.86 : size;
        head = new SvgView(assets, AssetIds.BOMBER_HEAD, team, headSize, headSize);
        skull = new SvgView(assets, AssetIds.ICON_SKULL, size * 0.45, size * 0.45);
        skull.setVisible(false);
        StackPane.setAlignment(skull, Pos.BOTTOM_RIGHT);
        getChildren().addAll(head, skull);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);
        if (boxed) {
            getStyleClass().add("avatar-box");
        }
        applyTeam(team);
    }

    public void setTeam(TeamColor team) {
        head.setTeam(team);
        applyTeam(team);
    }

    public void setState(State state) {
        this.state = state;
        boolean colourful = state == State.NORMAL;
        head.setEffect(colourful ? null : new ColorAdjust(0, -1, state == State.EMPTY ? -0.35 : 0, 0));
        head.setOpacity(state == State.EMPTY ? 0.45 : 1);
        skull.setVisible(state == State.OUT);
        applyTeam(head.getTeam());
    }

    public State getState() {
        return state;
    }

    private void applyTeam(TeamColor team) {
        if (!boxed) {
            return;
        }
        String fill = state == State.NORMAL && team != null
                ? "linear-gradient(to bottom, " + team.light() + ", " + team.main() + ")"
                : "linear-gradient(to bottom, #A7AABB, #8C8FA3)";
        setStyle("-avatar-fill: " + fill + ";");
    }
}
