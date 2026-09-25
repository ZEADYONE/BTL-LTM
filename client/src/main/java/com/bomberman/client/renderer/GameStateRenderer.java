package com.bomberman.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.PositionDto;
import com.bomberman.common.enums.TileType;

/** Primitive shape renderer for authoritative game snapshots. */
public final class GameStateRenderer implements AutoCloseable {

    private static final float TOP_UI_SPACE = 105f;
    private static final float MAP_PADDING = 20f;
    private final Color[] playerColors = {
            new Color(0.20f, 0.75f, 1f, 1f),
            new Color(1f, 0.78f, 0.12f, 1f),
            new Color(0.95f, 0.25f, 0.72f, 1f),
            new Color(0.30f, 0.90f, 0.35f, 1f)
    };

    private final ShapeRenderer shapes = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();

    public void render(GameStateDto state) {
        if (state == null || state.map().isEmpty()) {
            return;
        }

        int rows = state.map().size();
        int columns = state.map().getFirst().size();
        float tileSize = Math.min(
                (Gdx.graphics.getWidth() - MAP_PADDING * 2) / columns,
                (Gdx.graphics.getHeight() - TOP_UI_SPACE - MAP_PADDING) / rows
        );
        float originX = (Gdx.graphics.getWidth() - tileSize * columns) / 2f;
        float originY = MAP_PADDING;

        shapes.getProjectionMatrix().setToOrtho2D(
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                shapes.setColor(tileColor(state.map().get(row).get(column)));
                shapes.rect(
                        originX + column * tileSize,
                        originY + (rows - 1 - row) * tileSize,
                        tileSize - 1,
                        tileSize - 1
                );
            }
        }
        state.bombs().forEach(bomb -> {
            drawCircle(bomb.position(), rows, tileSize, originX, originY, Color.BLACK, 0.30f);
            drawCircle(bomb.position(), rows, tileSize, originX, originY, Color.WHITE, 0.08f);
        });
        state.explosions().forEach(explosion -> explosion.affectedPositions().forEach(position -> {
            shapes.setColor(new Color(1f, 0.45f, 0.05f, 0.8f));
            shapes.rect(
                    originX + position.column() * tileSize,
                    originY + (rows - 1 - position.row()) * tileSize,
                    tileSize - 1,
                    tileSize - 1
            );
        }));
        for (int index = 0; index < state.players().size(); index++) {
            var player = state.players().get(index);
            drawCircle(
                    player.position(),
                    rows,
                    tileSize,
                    originX,
                    originY,
                    playerColor(index, player.alive()),
                    0.34f
            );
        }

        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        drawPlayerNames(state, rows, tileSize, originX, originY);
    }

    public Color playerColor(int playerIndex, boolean alive) {
        return alive ? playerColors[playerIndex % playerColors.length] : Color.GRAY;
    }

    @Override
    public void close() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }

    private void drawCircle(
            PositionDto position,
            int rows,
            float tileSize,
            float originX,
            float originY,
            Color color,
            float radiusRatio
    ) {
        shapes.setColor(color);
        shapes.circle(
                originX + (position.column() + 0.5f) * tileSize,
                originY + (rows - position.row() - 0.5f) * tileSize,
                tileSize * radiusRatio
        );
    }

    private Color tileColor(TileType tile) {
        return switch (tile) {
            case EMPTY -> new Color(0.18f, 0.28f, 0.20f, 1f);
            case HARD_WALL -> new Color(0.32f, 0.36f, 0.42f, 1f);
            case BREAKABLE_WALL -> new Color(0.55f, 0.30f, 0.14f, 1f);
        };
    }

    private void drawPlayerNames(
            GameStateDto state,
            int rows,
            float tileSize,
            float originX,
            float originY
    ) {
        batch.getProjectionMatrix().setToOrtho2D(
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        batch.begin();
        for (int index = 0; index < state.players().size(); index++) {
            var player = state.players().get(index);
            font.setColor(playerColor(index, player.alive()));
            font.draw(
                    batch,
                    player.username() + (player.alive() ? "" : " (DEAD)"),
                    originX + player.position().column() * tileSize,
                    originY + (rows - player.position().row()) * tileSize + 14f
            );
        }
        batch.end();
    }
}
