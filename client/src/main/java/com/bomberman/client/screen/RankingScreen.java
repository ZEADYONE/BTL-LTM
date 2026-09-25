package com.bomberman.client.screen;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.bomberman.client.network.GameClientController;
import com.bomberman.client.state.ClientState;
import com.bomberman.common.dto.RankingEntryDto;

public final class RankingScreen extends BaseScreen {

    private final GameClientController controller;
    private final Table rankingTable = new Table();
    private final Label feedback;

    public RankingScreen(
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

        ScrollPane scrollPane = new ScrollPane(rankingTable, skin);
        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);
        root.add(new Label("Ranking", skin)).colspan(7).padBottom(12).row();
        root.add(scrollPane).colspan(7).expand().fill().row();
        root.add(feedback).colspan(6).left().padTop(8);
        root.add(back).width(180).height(42).right().padTop(8);
        stage.addActor(root);
    }

    @Override
    public void show() {
        super.show();
        controller.requestRanking();
    }

    @Override
    public void onClientStateChanged() {
        feedback.setText(state.getFeedback());
        rankingTable.clearChildren();
        rankingTable.defaults().pad(7).left();
        addHeader("#", "Player", "Score", "Wins", "Draws", "Losses");
        for (RankingEntryDto entry : state.getRankingEntries()) {
            rankingTable.add(new Label(Integer.toString(entry.rank()), skin)).width(45);
            rankingTable.add(new Label(entry.username(), skin)).width(220);
            rankingTable.add(new Label(formatScore(entry.totalScoreUnits()), skin)).width(90);
            rankingTable.add(new Label(Integer.toString(entry.totalWins()), skin)).width(70);
            rankingTable.add(new Label(Integer.toString(entry.totalDraws()), skin)).width(70);
            rankingTable.add(new Label(Integer.toString(entry.totalLosses()), skin)).width(70).row();
        }
        if (state.getRankingEntries().isEmpty()) {
            rankingTable.add(new Label("No ranked players yet", skin)).colspan(6).pad(20);
        }
    }

    private void addHeader(String... columns) {
        int[] widths = {45, 220, 90, 70, 70, 70};
        for (int index = 0; index < columns.length; index++) {
            rankingTable.add(new Label(columns[index], skin)).width(widths[index]);
        }
        rankingTable.row();
    }

    private String formatScore(long scoreUnits) {
        return String.format(java.util.Locale.ROOT, "%.1f", scoreUnits / 2.0);
    }
}
