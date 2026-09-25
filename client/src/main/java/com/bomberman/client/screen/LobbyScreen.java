package com.bomberman.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bomberman.client.network.GameClientController;
import com.bomberman.client.state.ClientState;
import com.bomberman.common.dto.OnlineUserDto;
import com.bomberman.common.dto.RoomSummaryDto;

public final class LobbyScreen extends BaseScreen {

    private final GameClientController controller;
    private final Label usernameLabel;
    private final Label feedbackLabel;
    private final Table usersTable = new Table();
    private final Table roomsTable = new Table();

    public LobbyScreen(
            ClientState state,
            ClientNavigator navigator,
            Skin skin,
            GameClientController controller
    ) {
        super(state, navigator, skin);
        this.controller = controller;

        usernameLabel = new Label("", skin);
        feedbackLabel = new Label("", skin);
        TextField roomName = new TextField("", skin);
        roomName.setMessageText("New room name");
        TextButton createRoom = new TextButton("Create Room", skin);
        TextButton ranking = new TextButton("Ranking", skin);
        TextButton history = new TextButton("History", skin);
        TextButton logout = new TextButton("Logout", skin);

        createRoom.addListener(change(() -> controller.createRoom(roomName.getText())));
        ranking.addListener(change(navigator::showRanking));
        history.addListener(change(navigator::showHistory));
        logout.addListener(change(() -> {
            controller.logout();
            navigator.showLogin();
        }));

        Table root = new Table();
        root.setFillParent(true);
        root.pad(18);
        root.add(new Label("Lobby", skin)).left().expandX();
        root.add(usernameLabel).right().padRight(12);
        root.add(logout).width(110).height(38).row();

        Table actions = new Table();
        actions.defaults().pad(5).height(40);
        actions.add(roomName).width(260);
        actions.add(createRoom).width(140);
        actions.add(ranking).width(110);
        actions.add(history).width(110);
        root.add(actions).colspan(3).left().row();

        usersTable.top().left();
        roomsTable.top().left();
        ScrollPane usersScroll = new ScrollPane(usersTable, skin);
        ScrollPane roomsScroll = new ScrollPane(roomsTable, skin);
        root.add(section("Online Users", usersScroll)).expand().fill().pad(8);
        root.add(section("Rooms", roomsScroll)).colspan(2).expand().fill().pad(8).row();
        root.add(feedbackLabel).colspan(3).left().padTop(6);
        stage.addActor(root);
    }

    @Override
    public void show() {
        super.show();
        controller.requestLobbyState();
    }

    @Override
    public void onClientStateChanged() {
        usernameLabel.setText("Player: " + valueOrDash(state.getCurrentUsername()));
        feedbackLabel.setText(state.getFeedback());
        rebuildUsers();
        rebuildRooms();
    }

    private void rebuildUsers() {
        usersTable.clearChildren();
        usersTable.defaults().left().pad(4);
        for (OnlineUserDto user : state.getOnlineUsers()) {
            usersTable.add(new Label(user.username() + "  [" + user.status() + "]", skin)).row();
        }
    }

    private void rebuildRooms() {
        roomsTable.clearChildren();
        roomsTable.defaults().left().pad(4);
        for (RoomSummaryDto room : state.getRooms()) {
            roomsTable.add(new Label(
                    room.roomName() + "  " + room.playerCount() + "/" + room.maxPlayers()
                            + "  [" + room.status() + "]",
                    skin
            )).width(360);
            TextButton join = new TextButton("Join", skin);
            join.setDisabled(room.playerCount() >= room.maxPlayers()
                    || room.status() != com.bomberman.common.enums.RoomStatus.WAITING);
            join.addListener(change(() -> controller.joinRoom(room.roomId())));
            roomsTable.add(join).width(90).height(34).row();
        }
    }

    private Table section(String title, ScrollPane content) {
        Table section = new Table();
        section.add(new Label(title, skin)).left().row();
        section.add(content).expand().fill().minWidth(350);
        return section;
    }

    private ChangeListener change(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                action.run();
            }
        };
    }

    private String valueOrDash(String value) {
        return value == null ? "-" : value;
    }
}
