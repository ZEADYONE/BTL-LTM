package com.bomberman.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.bomberman.client.state.ClientState;
import com.bomberman.client.state.ClientStateListener;

abstract class BaseScreen extends ScreenAdapter implements ClientStateListener {

    protected final ClientState state;
    protected final ClientNavigator navigator;
    protected final Skin skin;
    protected final Stage stage = new Stage(new ScreenViewport());

    BaseScreen(ClientState state, ClientNavigator navigator, Skin skin) {
        this.state = state;
        this.navigator = navigator;
        this.skin = skin;
    }

    @Override
    public void show() {
        state.addListener(this);
        Gdx.input.setInputProcessor(stage);
        onClientStateChanged();
    }

    @Override
    public void hide() {
        state.removeListener(this);
    }

    @Override
    public void render(float delta) {
        clearScreen();
        drawStage(delta);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        state.removeListener(this);
        stage.dispose();
    }

    protected void clearScreen() {
        Gdx.gl.glClearColor(0.035f, 0.05f, 0.09f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    protected void drawStage(float delta) {
        stage.act(delta);
        stage.draw();
    }
}
