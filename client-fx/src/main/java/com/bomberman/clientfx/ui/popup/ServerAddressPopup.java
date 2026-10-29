package com.bomberman.clientfx.ui.popup;

import com.bomberman.clientfx.network.ClientNetworkConfig;
import com.bomberman.clientfx.network.GameClientController;
import com.bomberman.clientfx.state.UserPreferences;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.GameFields;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.Optional;

import static com.bomberman.clientfx.ui.InputValidation.hostError;
import static com.bomberman.clientfx.ui.InputValidation.portError;

/** Chooses the game server from the Login screen (docs/ui-redesign/02, section 4.2). */
public final class ServerAddressPopup {

    private ServerAddressPopup() {
    }

    /** @param onChanged runs after the address was saved or reset, e.g. to refresh the Login screen */
    public static void show(AppShell shell, GameClientController controller, UserPreferences preferences,
                            Runnable onChanged) {
        ClientNetworkConfig current = controller.networkConfig();
        TextField host = GameFields.text("Host or IP, e.g. 192.168.1.20");
        host.setText(current.host());
        TextField port = GameFields.text("Port");
        port.setText(Integer.toString(current.port()));
        port.setPrefColumnCount(6);
        Label error = new Label();
        error.getStyleClass().add("field-error");

        GridPane fields = new GridPane();
        fields.setHgap(12);
        fields.setVgap(8);
        fields.add(heading("HOST"), 0, 0);
        fields.add(host, 1, 0);
        fields.add(heading("PORT"), 0, 1);
        fields.add(port, 1, 1);
        host.setPrefWidth(300);

        GameButton reset = new GameButton("RESET", GameButton.Tone.GREY, GameButton.Size.M);
        reset.setOnAction(event -> {
            preferences.clearServer();
            ClientNetworkConfig defaults = ClientNetworkConfig.load(null);
            controller.setNetworkConfig(defaults);
            host.setText(defaults.host());
            port.setText(Integer.toString(defaults.port()));
            error.setText("");
            onChanged.run();
        });
        GameButton cancel = new GameButton("CANCEL", GameButton.Tone.BLUE, GameButton.Size.M);
        cancel.setOnAction(event -> shell.closeModal());
        GameButton save = new GameButton("SAVE", GameButton.Tone.YELLOW, GameButton.Size.M);
        save.setDefaultButton(true);
        save.setOnAction(event -> {
            Optional<String> hostProblem = hostError(host.getText());
            Optional<String> portProblem = portError(port.getText());
            GameFields.markError(host, hostProblem.isPresent());
            GameFields.markError(port, portProblem.isPresent());
            error.setText(hostProblem.or(() -> portProblem).orElse(""));
            if (hostProblem.isEmpty() && portProblem.isEmpty()) {
                ClientNetworkConfig chosen = new ClientNetworkConfig(host.getText().strip(),
                        Integer.parseInt(port.getText().strip()));
                preferences.saveServer(chosen);
                controller.setNetworkConfig(chosen);
                shell.closeModal();
                onChanged.run();
            }
        });

        VBox body = new VBox(10, fields, error);
        shell.showModal(Popups.frame("SERVER ADDRESS", 520, body, reset, cancel, save), true);
        host.requestFocus();
    }

    private static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("popup-heading");
        return label;
    }
}
