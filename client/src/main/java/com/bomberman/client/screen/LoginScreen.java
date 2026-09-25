package com.bomberman.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bomberman.client.network.GameClientController;
import com.bomberman.client.state.ClientState;

public final class LoginScreen extends BaseScreen {

    private final Label feedbackLabel;

    public LoginScreen(
            ClientState state,
            ClientNavigator navigator,
            Skin skin,
            GameClientController controller
    ) {
        super(state, navigator, skin);

        TextField username = new TextField("", skin);
        username.setMessageText("Username");
        TextField password = new TextField("", skin);
        password.setMessageText("Password");
        password.setPasswordMode(true);
        password.setPasswordCharacter('*');

        TextButton login = new TextButton("Login", skin);
        TextButton register = new TextButton("Register", skin);
        feedbackLabel = new Label("", skin);
        feedbackLabel.setWrap(true);

        login.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                controller.login(username.getText(), password.getText());
            }
        });
        register.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                controller.register(username.getText(), password.getText());
            }
        });

        Table form = new Table();
        form.setFillParent(true);
        form.defaults().pad(8).width(320);
        form.add(new Label("Bomberman Online Mini", skin)).padBottom(24).row();
        form.add(username).height(42).row();
        form.add(password).height(42).row();

        Table buttons = new Table();
        buttons.defaults().pad(4).width(150).height(42);
        buttons.add(login);
        buttons.add(register);
        form.add(buttons).row();
        form.add(feedbackLabel).width(420).row();
        stage.addActor(form);
    }

    @Override
    public void onClientStateChanged() {
        feedbackLabel.setText(state.getFeedback());
    }
}
