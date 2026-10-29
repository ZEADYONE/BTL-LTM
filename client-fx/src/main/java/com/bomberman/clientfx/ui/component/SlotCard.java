package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

/** One of the four player slots in the Room Lobby (mockup 4), coloured by slot order. */
public final class SlotCard extends StackPane {

    public static final double WIDTH = 287;
    public static final double HEIGHT = 187;

    private final TeamColor team;
    private final Pane characterLayer = new Pane();
    private final Label number = new Label();
    private final Tag hostTag;
    private final Tag youTag = new Tag("YOU", Tag.Kind.YOU);
    private final GameButton kick = new GameButton(
            "",
            GameButton.Tone.RED,
            GameButton.Size.ICON,
            icon("mdi2c-close-thick", 19)
    );
    private final Label name = new Label();
    private final HBox status = new HBox();
    private final Label statusText = new Label();
    private final FontIcon statusIcon = new FontIcon();
    private final VBox playerInfo;
    private final VBox emptyInfo;

    public SlotCard(SvgAssets assets, int slotIndex) {
        team = TeamColor.forSlot(slotIndex);
        setMinSize(WIDTH, HEIGHT);
        setPrefSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        SvgView character = new SvgView(assets, AssetIds.BOMBER_FULL, team, 236, 236);
        character.relocate((WIDTH - 236) / 2, -4);
        characterLayer.getChildren().add(character);
        // Stop above the card's bottom edge so feet and gloves never peek out below the status bar.
        Rectangle clip = new Rectangle(WIDTH - 6, HEIGHT - 22);
        clip.setArcWidth(30);
        clip.setArcHeight(30);
        clip.relocate(3, 3);
        characterLayer.setClip(clip);

        number.setText(Integer.toString(slotIndex + 1));
        number.getStyleClass().add("slot-number");
        StackPane.setAlignment(number, Pos.TOP_LEFT);
        StackPane.setMargin(number, new Insets(10));

        hostTag = new Tag("HOST", Tag.Kind.HOST, new SvgView(assets, AssetIds.ICON_CROWN, 22, 22));
        StackPane.setAlignment(hostTag, Pos.TOP_CENTER);
        StackPane.setMargin(hostTag, new Insets(8, 0, 0, 0));
        StackPane.setAlignment(youTag, Pos.TOP_RIGHT);
        StackPane.setMargin(youTag, new Insets(10));
        kick.getStyleClass().add("slot-kick-button");
        kick.setMinSize(42, 42);
        kick.setPrefSize(42, 42);
        kick.setMaxSize(42, 42);
        StackPane.setAlignment(kick, Pos.TOP_RIGHT);
        StackPane.setMargin(kick, new Insets(8));

        name.getStyleClass().add("label-name");
        HBox nameBar = new HBox(name);
        nameBar.getStyleClass().add("name-bar");
        status.getStyleClass().add("status-bar");
        status.getChildren().addAll(statusIcon, statusText);
        statusIcon.setIconSize(18);
        statusIcon.setIconColor(Color.WHITE);
        playerInfo = new VBox(4, nameBar, status);
        playerInfo.setAlignment(Pos.BOTTOM_CENTER);
        playerInfo.setPadding(new Insets(0, 12, 12, 12));
        playerInfo.setMaxHeight(USE_PREF_SIZE);
        StackPane.setAlignment(playerInfo, Pos.BOTTOM_CENTER);

        PlayerHead silhouette = new PlayerHead(assets, team, 84, false);
        silhouette.setState(PlayerHead.State.EMPTY);
        Label waiting = new Label("WAITING…");
        waiting.getStyleClass().add("slot-waiting");
        emptyInfo = new VBox(8, silhouette, waiting);
        emptyInfo.setAlignment(Pos.CENTER);

        getChildren().addAll(characterLayer, emptyInfo, playerInfo, number, hostTag, youTag, kick);
        showEmpty();
    }

    public void showPlayer(String playerName, boolean ready, boolean host, boolean you) {
        showPlayer(playerName, ready, host, you, false, false, null);
    }

    public void showPlayer(
            String playerName,
            boolean ready,
            boolean host,
            boolean you,
            boolean kickable,
            boolean kickLoading,
            Runnable onKick
    ) {
        getStyleClass().setAll("slot-card");
        setStyle("-slot-fill: linear-gradient(to bottom, " + team.light() + ", " + team.main() + ");");
        characterLayer.setVisible(true);
        playerInfo.setVisible(true);
        emptyInfo.setVisible(false);
        name.setText(playerName);
        status.getStyleClass().setAll("status-bar");
        if (ready) {
            status.getStyleClass().add("ready");
        }
        statusIcon.setIconLiteral(ready ? "mdi2c-check-circle" : "mdi2c-checkbox-blank-circle-outline");
        statusText.setText(ready ? "READY" : "NOT READY");
        hostTag.setVisible(host);
        youTag.setVisible(you);
        kick.setVisible(kickable);
        kick.setManaged(kickable);
        kick.setLoading(kickLoading);
        kick.setTooltip(kickable ? new Tooltip("Remove " + playerName) : null);
        kick.setAccessibleText(kickable ? "Remove " + playerName : null);
        kick.setOnAction(kickable && onKick != null ? event -> onKick.run() : null);
    }

    public void showEmpty() {
        getStyleClass().setAll("slot-card", "empty");
        setStyle(null);
        characterLayer.setVisible(false);
        playerInfo.setVisible(false);
        emptyInfo.setVisible(true);
        hostTag.setVisible(false);
        youTag.setVisible(false);
        kick.setVisible(false);
        kick.setManaged(false);
        kick.setLoading(false);
        kick.setTooltip(null);
        kick.setAccessibleText(null);
        kick.setOnAction(null);
    }

    private static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }
}
