package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.ClientState;
import com.bomberman.clientfx.state.ClientStateListener;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.GameFields;
import com.bomberman.clientfx.ui.component.GameTabs;
import com.bomberman.clientfx.ui.component.HeroCharacter;
import com.bomberman.clientfx.ui.component.OutlinedText;
import com.bomberman.clientfx.ui.component.PasswordInput;
import com.bomberman.clientfx.ui.component.StatusDot;
import com.bomberman.clientfx.ui.popup.ServerAddressPopup;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.bomberman.clientfx.ui.InputValidation.confirmationError;
import static com.bomberman.clientfx.ui.InputValidation.passwordError;
import static com.bomberman.clientfx.ui.InputValidation.usernameError;

/** S1 Login / Register (docs/ui-redesign/02, section 3.1). */
public final class LoginScreen implements Screen {

    private static final int LOGIN_TAB = 0;
    private static final int REGISTER_TAB = 1;

    private final ClientState state;
    private final GameClientController controller;
    private final UserPreferences preferences;
    private final AnchorPane root = new AnchorPane();
    private final HeroCharacter hero;
    private final GameTabs tabs = new GameTabs("LOGIN", "REGISTER");
    private final TextField username = GameFields.text("Your player name");
    private final PasswordInput password = new PasswordInput("Password");
    private final PasswordInput confirmation = new PasswordInput("Type the password again");
    private final VBox confirmationGroup;
    private final Label inputError = new Label();
    private final GameButton submit = new GameButton("LOGIN", GameButton.Tone.YELLOW, GameButton.Size.L);
    private final StatusDot connectionDot = new StatusDot(false);
    private final Label serverAddress = new Label();
    private final ClientStateListener stateListener = this::onStateChanged;

    public LoginScreen(ClientState state, GameClientController controller, UserPreferences preferences,
                       SvgAssets assets, AppShell shell) {
        this.state = state;
        this.controller = controller;
        this.preferences = preferences;

        VBox logo = new VBox(-6,
                new OutlinedText("BOMBERMAN", OutlinedText.Style.TITLE),
                new OutlinedText("ONLINE", OutlinedText.Style.HEADING, OutlinedText.GOLD));
        logo.setAlignment(Pos.CENTER);
        hero = new HeroCharacter(assets, TeamColor.RED, 330);
        VBox showcase = new VBox(6, logo, hero);
        showcase.setAlignment(Pos.TOP_CENTER);
        AnchorPane.setLeftAnchor(showcase, 70.0);
        AnchorPane.setTopAnchor(showcase, 36.0);

        username.setText(preferences.lastUsername());
        confirmationGroup = new VBox(6, formLabel("CONFIRM PASSWORD"), confirmation);
        inputError.getStyleClass().add("field-error");
        inputError.setWrapText(true);
        inputError.managedProperty().bind(inputError.textProperty().isNotEmpty());
        submit.setDefaultButton(true);
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(event -> submit());

        HBox tabRow = new HBox(tabs);
        tabRow.setAlignment(Pos.CENTER);
        VBox card = new VBox(12,
                tabRow,
                new VBox(6, formLabel("USERNAME"), username),
                new VBox(6, formLabel("PASSWORD"), password),
                confirmationGroup,
                inputError,
                submit);
        card.getStyleClass().addAll("panel", "login-card");
        card.setPrefWidth(460);
        AnchorPane.setRightAnchor(card, 70.0);
        AnchorPane.setTopAnchor(card, 70.0);

        GameButton serverButton = new GameButton("", GameButton.Tone.PURPLE, GameButton.Size.ICON, icon("mdi2c-cog"));
        serverButton.setOnAction(event -> ServerAddressPopup.show(shell, controller, preferences, this::refreshServer));
        serverAddress.getStyleClass().add("server-address");
        HBox serverLine = new HBox(10, connectionDot, serverAddress, serverButton);
        serverLine.setAlignment(Pos.CENTER_LEFT);
        AnchorPane.setLeftAnchor(serverLine, 36.0);
        AnchorPane.setBottomAnchor(serverLine, 24.0);

        tabs.selectedIndexProperty().addListener((observable, previous, index) -> applyMode(index.intValue()));
        applyMode(LOGIN_TAB);
        username.textProperty().addListener(observable -> inputError.setText(""));
        password.textProperty().addListener(observable -> inputError.setText(""));
        confirmation.textProperty().addListener(observable -> inputError.setText(""));

        root.getChildren().addAll(showcase, card, serverLine);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onShow() {
        state.addListener(stateListener);
        hero.play();
        refreshServer();
        submit.setLoading(false);
        password.clear();
        confirmation.clear();
        inputError.setText("");
        if (username.getText().isEmpty()) {
            username.requestFocus();
        } else {
            password.focusField();
        }
    }

    @Override
    public void onHide() {
        state.removeListener(stateListener);
        hero.stop();
    }

    private void applyMode(int tab) {
        boolean register = tab == REGISTER_TAB;
        confirmationGroup.setVisible(register);
        confirmationGroup.setManaged(register);
        submit.setLabelText(register ? "CREATE ACCOUNT" : "LOGIN");
        inputError.setText("");
        confirmation.clear();
    }

    private void submit() {
        boolean register = tabs.selectedIndexProperty().get() == REGISTER_TAB;
        String name = username.getText();
        String secret = password.getText();
        Optional<String> nameProblem = usernameError(name);
        Optional<String> passwordProblem = passwordError(secret);
        Optional<String> confirmationProblem = register
                ? confirmationError(secret, confirmation.getText())
                : Optional.empty();
        GameFields.markError(username, nameProblem.isPresent());
        password.markError(passwordProblem.isPresent());
        confirmation.markError(confirmationProblem.isPresent());
        Optional<String> problem = nameProblem.or(() -> passwordProblem).or(() -> confirmationProblem);
        inputError.setText(problem.orElse(""));
        if (problem.isPresent()) {
            return;
        }

        submit.setLoading(true);
        CompletableFuture<NetworkMessage> reply = register
                ? controller.register(name, secret)
                : controller.login(name, secret);
        reply.whenComplete((message, failure) -> {
            submit.setLoading(false);
            if (message != null && message.type() == MessageType.REGISTER_RESPONSE
                    && message.payload().path("success").asBoolean(false)) {
                tabs.select(LOGIN_TAB);
                password.clear();
                password.focusField();
            }
        });
    }

    private void onStateChanged() {
        connectionDot.setOn(state.isConnected());
        if (state.isLoggedIn()) {
            preferences.setLastUsername(state.getCurrentUsername());
        }
    }

    private void refreshServer() {
        serverAddress.setText(controller.networkConfig().displayAddress());
        connectionDot.setOn(state.isConnected());
    }

    private static Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("form-label");
        return label;
    }

    private static FontIcon icon(String literal) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(28);
        return icon;
    }
}
