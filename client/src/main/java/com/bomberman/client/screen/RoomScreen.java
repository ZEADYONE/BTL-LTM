package com.bomberman.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bomberman.client.network.GameClientController;
import com.bomberman.client.state.ClientState;
import com.bomberman.common.dto.RoomPlayerDto;
import com.bomberman.common.dto.RoomStateDto;

public final class RoomScreen extends BaseScreen {

    private final GameClientController controller;
    private final Label roomNameLabel;
    private final Label hostLabel;
    private final Label feedbackLabel;
    private final Table playersTable = new Table();
    private final TextButton readyButton;
    private final TextButton startButton;

    public RoomScreen(
            ClientState state,
            ClientNavigator navigator,
            Skin skin,
            GameClientController controller
    ) {
        super(state, navigator, skin);
        this.controller = controller;

        roomNameLabel = new Label("Room", skin);
        hostLabel = new Label("Host: -", skin);
        feedbackLabel = new Label("", skin);
        readyButton = new TextButton("Ready", skin);
        startButton = new TextButton("Start Game", skin);
        TextButton leaveButton = new TextButton("Leave Room", skin);

        readyButton.addListener(change(() -> controller.ready(!isCurrentPlayerReady())));
        startButton.addListener(change(controller::startGame));
        leaveButton.addListener(change(controller::leaveRoom));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(24);
        root.add(roomNameLabel).left().expandX();
        root.add(hostLabel).right().row();
        root.add(playersTable).colspan(2).expand().fill().pad(20).row();

        Table buttons = new Table();
        buttons.defaults().pad(6).width(150).height(42);
        buttons.add(readyButton);
        buttons.add(startButton);
        buttons.add(leaveButton);
        root.add(buttons).colspan(2).row();
        root.add(feedbackLabel).colspan(2).left().padTop(8);
        stage.addActor(root);
    }

    @Override
    public void onClientStateChanged() {
        RoomStateDto room = state.getRoom();
        playersTable.clearChildren();
        if (room == null) {
            roomNameLabel.setText("Room");
            hostLabel.setText("Host: -");
            return;
        }

        roomNameLabel.setText(room.roomName() + "  [" + room.status() + "]");
        String hostName = room.players().stream()
                .filter(player -> player.userId() == room.hostUserId())
                .map(RoomPlayerDto::username)
                .findFirst()
                .orElse("-");
        hostLabel.setText("Host: " + hostName);
        playersTable.defaults().left().pad(6);
        for (RoomPlayerDto player : room.players()) {
            String marker = player.userId() == room.hostUserId() ? "HOST" : "PLAYER";
            playersTable.add(new Label(
                    player.username() + "  [" + marker + "]  "
                            + (player.ready() ? "READY" : "NOT READY"),
                    skin
            )).row();
        }
        readyButton.setText(isCurrentPlayerReady() ? "Unready" : "Ready");
        startButton.setDisabled(state.getCurrentUserId() != room.hostUserId());
        feedbackLabel.setText(state.getFeedback());
    }

    private boolean isCurrentPlayerReady() {
        RoomStateDto room = state.getRoom();
        return room != null && room.players().stream()
                .filter(player -> player.userId() == state.getCurrentUserId())
                .map(RoomPlayerDto::ready)
                .findFirst()
                .orElse(false);
    }

    private ChangeListener change(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                action.run();
            }
        };
    }
}
