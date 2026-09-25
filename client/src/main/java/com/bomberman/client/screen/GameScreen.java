package com.bomberman.client.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bomberman.client.network.GameClientController;
import com.bomberman.client.renderer.GameStateRenderer;
import com.bomberman.client.state.ClientState;
import com.bomberman.common.dto.GameOverDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.enums.Direction;
import com.bomberman.common.enums.GameResult;
import com.bomberman.common.enums.GameStatus;

import java.util.Objects;

public final class GameScreen extends BaseScreen {

    private final GameClientController controller;
    private final GameStateRenderer gameRenderer = new GameStateRenderer();
    private final Label statusLabel;
    private final Label resultLabel;
    private final Table playerStatusTable = new Table();
    private final Table gameOverOverlay;

    private GameStateDto presentedState;
    private GameOverDto presentedGameOver;

    public GameScreen(
            ClientState state,
            ClientNavigator navigator,
            Skin skin,
            GameClientController controller
    ) {
        super(state, navigator, skin);
        this.controller = controller;
        statusLabel = new Label("Waiting for GAME_STATE...", skin);

        Table hud = new Table();
        hud.setFillParent(true);
        hud.top().pad(12);
        Table gameInfo = new Table();
        gameInfo.add(statusLabel).left().row();
        gameInfo.add(new Label("WASD / Arrow keys: Move    Space: Place bomb", skin)).left();
        hud.add(gameInfo).left().top().expandX();
        hud.add(playerStatusTable).right().top();
        stage.addActor(hud);

        resultLabel = new Label("", skin);
        TextButton playAgainButton = new TextButton("Play Again", skin);
        TextButton leaveRoomButton = new TextButton("Leave Room", skin);
        playAgainButton.addListener(change(controller::playAgain));
        leaveRoomButton.addListener(change(controller::leaveRoom));

        Table resultPanel = new Table();
        resultPanel.setBackground(skin.newDrawable(
                "white",
                new com.badlogic.gdx.graphics.Color(0.03f, 0.05f, 0.09f, 0.94f)
        ));
        resultPanel.defaults().pad(12);
        resultPanel.add(new Label("GAME OVER", skin)).colspan(2).row();
        resultPanel.add(resultLabel).colspan(2).row();
        resultPanel.add(playAgainButton).width(150).height(44);
        resultPanel.add(leaveRoomButton).width(150).height(44);

        gameOverOverlay = new Table();
        gameOverOverlay.setFillParent(true);
        gameOverOverlay.add(resultPanel).width(390).pad(20);
        gameOverOverlay.setVisible(false);
        stage.addActor(gameOverOverlay);
    }

    @Override
    public void render(float delta) {
        GameStateDto snapshot = state.getGameState();
        presentIfChanged(snapshot, state.getGameOver());
        handleInput(snapshot);
        clearScreen();
        gameRenderer.render(snapshot);
        drawStage(delta);
    }

    @Override
    public void onClientStateChanged() {
        presentIfChanged(state.getGameState(), state.getGameOver());
    }

    private void presentIfChanged(GameStateDto snapshot, GameOverDto gameOver) {
        if (presentedState == snapshot && presentedGameOver == gameOver) {
            return;
        }
        presentedState = snapshot;
        presentedGameOver = gameOver;
        if (snapshot == null) {
            statusLabel.setText("Waiting for GAME_STATE...");
            playerStatusTable.clearChildren();
            gameOverOverlay.setVisible(false);
        } else {
            statusLabel.setText(
                    "Tick: " + snapshot.tick()
                            + "    Status: " + snapshot.gameStatus()
                            + "    Alive: " + snapshot.remainingPlayers()
            );
            rebuildPlayerStatus(snapshot);
            presentGameOver(snapshot, gameOver);
        }
    }

    private void rebuildPlayerStatus(GameStateDto snapshot) {
        playerStatusTable.clearChildren();
        playerStatusTable.defaults().right().pad(2);
        for (int index = 0; index < snapshot.players().size(); index++) {
            GamePlayerStateDto player = snapshot.players().get(index);
            Label label = new Label(
                    player.username() + "  " + (player.alive() ? "ALIVE" : "DEAD"),
                    skin
            );
            label.setColor(gameRenderer.playerColor(index, player.alive()));
            playerStatusTable.add(label).right().row();
        }
    }

    private void presentGameOver(GameStateDto snapshot, GameOverDto gameOver) {
        boolean finished = snapshot.gameStatus() == GameStatus.FINISHED;
        gameOverOverlay.setVisible(finished);
        if (!finished) {
            return;
        }
        if (gameOver == null) {
            resultLabel.setText("Waiting for server result...");
            resultLabel.setColor(com.badlogic.gdx.graphics.Color.LIGHT_GRAY);
            return;
        }
        GameResult result = gameOver.players().stream()
                .filter(player -> player.userId() == state.getCurrentUserId())
                .findFirst()
                .map(com.bomberman.common.dto.GameOverPlayerDto::result)
                .orElse(null);
        if (result == null) {
            resultLabel.setText("Invalid server result");
            resultLabel.setColor(com.badlogic.gdx.graphics.Color.SCARLET);
            return;
        }
        resultLabel.setText(result == GameResult.LOSS ? "LOSE" : result.name());
        resultLabel.setColor(switch (result) {
            case WIN -> com.badlogic.gdx.graphics.Color.LIME;
            case LOSS -> com.badlogic.gdx.graphics.Color.SCARLET;
            case DRAW -> com.badlogic.gdx.graphics.Color.GOLD;
        });
    }

    @Override
    public void dispose() {
        super.dispose();
        gameRenderer.close();
    }

    private void handleInput(GameStateDto snapshot) {
        if (!canSendGameplayInput(snapshot)) {
            return;
        }
        if (pressed(Input.Keys.UP, Input.Keys.W)) {
            controller.move(Direction.UP);
        } else if (pressed(Input.Keys.DOWN, Input.Keys.S)) {
            controller.move(Direction.DOWN);
        } else if (pressed(Input.Keys.LEFT, Input.Keys.A)) {
            controller.move(Direction.LEFT);
        } else if (pressed(Input.Keys.RIGHT, Input.Keys.D)) {
            controller.move(Direction.RIGHT);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            controller.placeBomb();
        }
    }

    private boolean pressed(int firstKey, int secondKey) {
        return Gdx.input.isKeyJustPressed(firstKey) || Gdx.input.isKeyJustPressed(secondKey);
    }

    private boolean canSendGameplayInput(GameStateDto snapshot) {
        return snapshot != null
                && snapshot.gameStatus() == GameStatus.RUNNING
                && snapshot.players().stream().anyMatch(player ->
                        player.userId() == state.getCurrentUserId() && player.alive()
                );
    }

    private ChangeListener change(Runnable action) {
        Objects.requireNonNull(action);
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                action.run();
            }
        };
    }
}
