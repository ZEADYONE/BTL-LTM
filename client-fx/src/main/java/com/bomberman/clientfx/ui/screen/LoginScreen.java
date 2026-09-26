package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.clientfx.ui.Screen;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Optional;
import java.util.function.BiConsumer;

import static com.bomberman.clientfx.ui.InputValidation.passwordError;
import static com.bomberman.clientfx.ui.InputValidation.usernameError;

/** S1, temporary layout for phase 1; the styled version is built in phase 3. */
public final class LoginScreen implements Screen {

    private final ClientState state;
    private final UserPreferences preferences;
    private final StackPane root = new StackPane();
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label inputError = new Label();
    private final ClientStateListener rememberUsername = this::rememberUsernameAfterLogin;

    public LoginScreen(ClientState state, GameClientController controller, UserPreferences preferences) {
        this.state = state;
        this.preferences = preferences;

        Label title = new Label("BOMBERMAN ONLINE");
        title.getStyleClass().add("temp-title");
        Label note = new Label("Temporary login screen – the final design arrives in phase 3.");
        note.getStyleClass().add("temp-note");

        username.setPromptText("Username");
        username.setText(preferences.lastUsername());
        password.setPromptText("Password");

        Button login = new Button("LOGIN");
        login.setDefaultButton(true);
        login.setOnAction(event -> submit(controller::login));
        Button register = new Button("REGISTER");
        register.setOnAction(event -> submit(controller::register));
        HBox buttons = new HBox(12, login, register);
        buttons.setAlignment(Pos.CENTER);

        inputError.getStyleClass().add("temp-error");
        Label server = new Label("Server: " + controller.networkConfig().displayAddress());
        server.getStyleClass().add("temp-note");

        VBox panel = new VBox(12, title, note, username, password, buttons, inputError, server);
        panel.getStyleClass().add("temp-panel");
        panel.setAlignment(Pos.CENTER);
        panel.setMaxSize(420, VBox.USE_PREF_SIZE);
        root.getChildren().add(panel);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(rememberUsername);
        password.clear();
        inputError.setText("");
        (username.getText().isEmpty() ? username : password).requestFocus();
    }

    @Override
    public void onHide() {
        state.removeListener(rememberUsername);
    }

    private void submit(BiConsumer<String, String> action) {
        String name = username.getText();
        String secret = password.getText();
        Optional<String> problem = usernameError(name).or(() -> passwordError(secret));
        inputError.setText(problem.orElse(""));
        if (problem.isEmpty()) {
            action.accept(name, secret);
        }
    }

    private void rememberUsernameAfterLogin() {
        if (state.isLoggedIn()) {
            preferences.setLastUsername(state.getCurrentUsername());
        }
    }
}
