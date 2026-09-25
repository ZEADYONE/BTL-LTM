package com.bomberman.client.asset;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;

/** Owns the minimal programmatic Scene2D skin used by the desktop client. */
public final class ClientAssets implements AutoCloseable {

    private final Skin skin = createSkin();

    public Skin skin() {
        return skin;
    }

    @Override
    public void close() {
        skin.dispose();
    }

    private Skin createSkin() {
        Skin result = new Skin();
        BitmapFont font = new BitmapFont();
        result.add("default", font, BitmapFont.class);

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture white = new Texture(pixmap);
        pixmap.dispose();
        result.add("white", white, Texture.class);

        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        result.add("default", labelStyle);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.up = result.newDrawable("white", new Color(0.15f, 0.32f, 0.55f, 1f));
        buttonStyle.down = result.newDrawable("white", new Color(0.08f, 0.20f, 0.38f, 1f));
        buttonStyle.over = result.newDrawable("white", new Color(0.20f, 0.42f, 0.68f, 1f));
        buttonStyle.disabled = result.newDrawable("white", new Color(0.20f, 0.20f, 0.20f, 1f));
        result.add("default", buttonStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = result.newDrawable("white", new Color(0.10f, 0.12f, 0.18f, 1f));
        textFieldStyle.cursor = result.newDrawable("white", Color.WHITE);
        textFieldStyle.selection = result.newDrawable("white", new Color(0.25f, 0.45f, 0.75f, 1f));
        result.add("default", textFieldStyle);

        ScrollPane.ScrollPaneStyle scrollPaneStyle = new ScrollPane.ScrollPaneStyle();
        scrollPaneStyle.background = result.newDrawable("white", new Color(0.06f, 0.08f, 0.12f, 0.9f));
        result.add("default", scrollPaneStyle);
        return result;
    }
}
