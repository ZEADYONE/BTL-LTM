package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Ranked player line used by the Result screen (mockup 6) and the Leaderboard. */
public final class RankRow extends HBox {

    public RankRow(SvgAssets assets, int rank, String playerName, TeamColor team, String score, Tag badge, boolean you) {
        getStyleClass().add("rank-row");
        if (rank == 1) {
            getStyleClass().add("first");
        }
        if (you) {
            getStyleClass().add("you");
        }

        Label rankBadge = new Label(Integer.toString(rank));
        rankBadge.getStyleClass().addAll("rank-badge", "rank-" + Math.min(rank, 4));

        Label name = new Label(playerName);
        name.getStyleClass().add("label-name");
        name.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label scoreText = new Label(score);
        scoreText.getStyleClass().add("score-text");

        getChildren().addAll(
                rankBadge,
                new PlayerHead(assets, team, 56, true),
                name,
                new SvgView(assets, AssetIds.ICON_STAR, 34, 34),
                scoreText
        );
        if (badge != null) {
            getChildren().add(badge);
        }
        if (you) {
            getChildren().add(new Tag("YOU", Tag.Kind.YOU));
        }
    }
}
