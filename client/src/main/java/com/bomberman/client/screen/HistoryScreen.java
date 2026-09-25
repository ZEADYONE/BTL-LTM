package com.bomberman.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bomberman.client.network.GameClientController;
import com.bomberman.client.state.ClientState;
import com.bomberman.common.dto.MatchHistoryEntryDto;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;

public final class HistoryScreen extends BaseScreen {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final GameClientController controller;
    private final Table historyTable = new Table();
    private final Label feedback;

    public HistoryScreen(
            ClientState state,
            ClientNavigator navigator,
            Skin skin,
            GameClientController controller
    ) {
        super(state, navigator, skin);
        this.controller = controller;
        feedback = new Label("", skin);

        TextButton back = new TextButton("Back to Lobby", skin);
        back.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                navigator.showLobby();
            }
        });

        ScrollPane scrollPane = new ScrollPane(historyTable, skin);
        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);
        root.add(new Label("Match History", skin)).colspan(4).padBottom(12).row();
        root.add(scrollPane).colspan(4).expand().fill().row();
        root.add(feedback).colspan(3).left().padTop(8);
        root.add(back).width(180).height(42).right().padTop(8);
        stage.addActor(root);
    }

    @Override
    public void show() {
        super.show();
        controller.requestHistory();
    }

    @Override
    public void onClientStateChanged() {
        feedback.setText(state.getFeedback());
        historyTable.clearChildren();
        historyTable.defaults().pad(7).left();
        addRow("Date", "Players", "Result", "Score");
        for (MatchHistoryEntryDto match : state.getMatchHistory()) {
            addRow(
                    DATE_FORMAT.format(Instant.ofEpochMilli(match.endedAtEpochMillis())),
                    match.players().stream()
                            .map(player -> player.username() + " (" + player.result() + ")")
                            .collect(Collectors.joining(", ")),
                    match.viewerResult().name(),
                    formatScore(match.viewerScoreEarnedUnits())
            );
        }
        if (state.getMatchHistory().isEmpty()) {
            historyTable.add(new Label("No completed matches yet", skin)).colspan(4).pad(20);
        }
    }

    private void addRow(String date, String players, String result, String score) {
        historyTable.add(new Label(date, skin)).width(155);
        Label playersLabel = new Label(players, skin);
        playersLabel.setWrap(true);
        historyTable.add(playersLabel).width(430);
        historyTable.add(new Label(result, skin)).width(80);
        historyTable.add(new Label(score, skin)).width(70).row();
    }

    private String formatScore(int scoreUnits) {
        return String.format(Locale.ROOT, "%.1f", scoreUnits / 2.0);
    }
}
