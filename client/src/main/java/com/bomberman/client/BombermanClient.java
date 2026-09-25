package com.bomberman.client;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.bomberman.client.asset.ClientAssets;
import com.bomberman.client.network.ClientMessageDispatcher;
import com.bomberman.client.network.GameClientController;
import com.bomberman.client.network.GameNetworkClient;
import com.bomberman.client.network.ClientNetworkConfig;
import com.bomberman.client.screen.ClientNavigator;
import com.bomberman.client.screen.GameScreen;
import com.bomberman.client.screen.HistoryScreen;
import com.bomberman.client.screen.LobbyScreen;
import com.bomberman.client.screen.LoginScreen;
import com.bomberman.client.screen.RankingScreen;
import com.bomberman.client.screen.RoomScreen;
import com.bomberman.client.state.ClientState;

public class BombermanClient extends Game implements ClientNavigator {

    private ClientAssets assets;
    private ClientState state;
    private GameNetworkClient networkClient;
    private GameClientController controller;

    private LoginScreen loginScreen;
    private LobbyScreen lobbyScreen;
    private RoomScreen roomScreen;
    private GameScreen gameScreen;
    private RankingScreen rankingScreen;
    private HistoryScreen historyScreen;

    @Override
    public void create() {
        assets = new ClientAssets();
        state = new ClientState();

        ClientMessageDispatcher dispatcher = new ClientMessageDispatcher(state, this);
        networkClient = new GameNetworkClient(dispatcher);
        controller = new GameClientController(
                networkClient,
                state,
                ClientNetworkConfig.load()
        );

        loginScreen = new LoginScreen(state, this, assets.skin(), controller);
        lobbyScreen = new LobbyScreen(state, this, assets.skin(), controller);
        roomScreen = new RoomScreen(state, this, assets.skin(), controller);
        gameScreen = new GameScreen(state, this, assets.skin(), controller);
        rankingScreen = new RankingScreen(state, this, assets.skin(), controller);
        historyScreen = new HistoryScreen(state, this, assets.skin(), controller);

        showLogin();
    }

    @Override
    public void showLogin() {
        switchTo(loginScreen);
    }

    @Override
    public void showLobby() {
        switchTo(lobbyScreen);
    }

    @Override
    public void showRoom() {
        switchTo(roomScreen);
    }

    @Override
    public void showGame() {
        switchTo(gameScreen);
    }

    @Override
    public void showRanking() {
        switchTo(rankingScreen);
    }

    @Override
    public void showHistory() {
        switchTo(historyScreen);
    }

    private void switchTo(Screen target) {
        if (target != null && getScreen() != target) {
            setScreen(target);
        }
    }

    @Override
    public void dispose() {
        if (controller != null) {
            controller.close();
        }
        if (networkClient != null) {
            networkClient.close();
        }
        disposeScreen(loginScreen);
        disposeScreen(lobbyScreen);
        disposeScreen(roomScreen);
        disposeScreen(gameScreen);
        disposeScreen(rankingScreen);
        disposeScreen(historyScreen);
        if (assets != null) {
            assets.close();
        }
    }

    private void disposeScreen(Screen target) {
        if (target != null) {
            target.dispose();
        }
    }
}
