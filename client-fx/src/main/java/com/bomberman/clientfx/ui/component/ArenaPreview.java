package com.bomberman.clientfx.ui.component;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.clientfx.ServerRules.Cell;
import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.game.MapPreview;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.enums.TileType;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;

/**
 * Miniature of the default arena drawn with the real tile art, with a head on each occupied
 * spawn so players see where they will start (R-10). Static: no animation, safe anywhere.
 */
public final class ArenaPreview extends Pane {

    private static final double BLOCK_HEIGHT_RATIO = 80.0 / 64.0;
    private static final Color GRASS_LIGHT = Color.web("#7CC04B");
    private static final Color GRASS_DARK = Color.web("#72B545");

    private final List<PlayerHead> spawnHeads = new ArrayList<>();

    public ArenaPreview(SvgAssets assets, double tile) {
        double overhang = tile * (BLOCK_HEIGHT_RATIO - 1);
        TileType[][] map = MapPreview.defaultMap();
        for (int row = 0; row < map.length; row++) {
            for (int column = 0; column < map[row].length; column++) {
                Rectangle floor = new Rectangle(tile, tile, (row + column) % 2 == 0 ? GRASS_LIGHT : GRASS_DARK);
                floor.relocate(column * tile, overhang + row * tile);
                getChildren().add(floor);
            }
        }
        for (int row = 0; row < map.length; row++) {
            for (int column = 0; column < map[row].length; column++) {
                TileType tileType = map[row][column];
                if (tileType == TileType.EMPTY) {
                    continue;
                }
                String asset = tileType == TileType.HARD_WALL ? AssetIds.BLOCK_STONE : AssetIds.CRATE;
                SvgView block = new SvgView(assets, asset, tile, tile * BLOCK_HEIGHT_RATIO);
                block.relocate(column * tile, row * tile);
                getChildren().add(block);
            }
        }
        for (int slot = 0; slot < ServerRules.SPAWNS.size(); slot++) {
            Cell spawn = ServerRules.SPAWNS.get(slot);
            PlayerHead head = new PlayerHead(assets, TeamColor.forSlot(slot), tile * 1.25, false);
            head.relocate(spawn.column() * tile - tile * 0.125, overhang + spawn.row() * tile - tile * 0.2);
            head.setVisible(false);
            spawnHeads.add(head);
            getChildren().add(head);
        }
        double width = tile * ServerRules.MAP_COLUMNS;
        double height = overhang + tile * ServerRules.MAP_ROWS;
        setMinSize(width, height);
        setPrefSize(width, height);
        setMaxSize(width, height);
        setMouseTransparent(true);
    }

    /** Shows heads on the first {@code players} spawns (room order = spawn order). */
    public void showPlayers(int players) {
        for (int slot = 0; slot < spawnHeads.size(); slot++) {
            spawnHeads.get(slot).setVisible(slot < players);
        }
    }
}
