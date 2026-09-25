package com.bomberman.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

public final class TitleScreen extends ScreenAdapter {

    private static final String TITLE = "Bomberman Online Mini";

    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout titleLayout = new GlyphLayout();

    public TitleScreen() {
        font.setColor(Color.WHITE);
        font.getData().setScale(2.5f);
        titleLayout.setText(font, TITLE);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.07f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float x = (Gdx.graphics.getWidth() - titleLayout.width) / 2f;
        float y = (Gdx.graphics.getHeight() + titleLayout.height) / 2f;

        batch.begin();
        font.draw(batch, titleLayout, x, y);
        batch.end();
    }

    @Override
    public void dispose() {
        dispose(batch);
        dispose(font);
    }

    private static void dispose(Disposable resource) {
        resource.dispose();
    }
}
