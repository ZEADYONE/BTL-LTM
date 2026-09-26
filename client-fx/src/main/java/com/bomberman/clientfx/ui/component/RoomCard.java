package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.enums.RoomStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Room entry in the Room Browser: name, host, occupied slots, status and JOIN. */
public final class RoomCard extends VBox {

    private static final int SLOTS = 4;

    private final Label name = new Label();
    private final Label host = new Label();
    private final Label count = new Label();
    private final List<PlayerHead> heads = new ArrayList<>();
    private final Tag status = new Tag("", Tag.Kind.WAITING);
    private final GameButton join = new GameButton("JOIN", GameButton.Tone.GREEN, GameButton.Size.S);

    public RoomCard(SvgAssets assets) {
        getStyleClass().add("room-card");
        name.getStyleClass().add("label-name");
        host.getStyleClass().add("label-caption");
        count.getStyleClass().add("label-display");
        count.setStyle("-fx-text-fill: -color-outline; -fx-font-size: 18px;");

        HBox headRow = new HBox(4);
        headRow.setAlignment(Pos.CENTER_LEFT);
        for (int slot = 0; slot < SLOTS; slot++) {
            PlayerHead head = new PlayerHead(assets, TeamColor.forSlot(slot), 30, false);
            heads.add(head);
            headRow.getChildren().add(head);
        }
        headRow.getChildren().add(count);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox footer = new HBox(10, status, spacer, join);
        footer.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(name, host, headRow, footer);
    }

    public void update(String roomName, String hostName, int players, int maxPlayers, RoomStatus roomStatus) {
        name.setText(roomName);
        host.setText(hostName == null ? "" : "host: " + hostName);
        host.setManaged(hostName != null);
        host.setVisible(hostName != null);
        count.setText(players + "/" + maxPlayers);
        for (int slot = 0; slot < heads.size(); slot++) {
            heads.get(slot).setState(slot < players ? PlayerHead.State.NORMAL : PlayerHead.State.EMPTY);
        }
        status.setText(roomStatus.name());
        status.setKind(switch (roomStatus) {
            case WAITING -> Tag.Kind.WAITING;
            case PLAYING -> Tag.Kind.PLAYING;
            case FINISHED -> Tag.Kind.FINISHED;
        });
        join.setDisable(roomStatus != RoomStatus.WAITING || players >= maxPlayers);
    }

    public void setOnJoin(Runnable action) {
        join.setOnAction(event -> action.run());
    }
}
