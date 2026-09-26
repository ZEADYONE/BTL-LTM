package com.bomberman.clientfx.game;

import com.bomberman.clientfx.ServerRules;
import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.game.FlameClassifier.Piece;
import com.bomberman.clientfx.game.MatchTracker.FrameEvents;
import com.bomberman.clientfx.game.fx.ParticlePool;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.dto.BombStateDto;
import com.bomberman.common.dto.ExplosionStateDto;
import com.bomberman.common.dto.GamePlayerStateDto;
import com.bomberman.common.dto.GameStateDto;
import com.bomberman.common.dto.PositionDto;
import com.bomberman.common.enums.TileType;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Random;

/** Canvas renderer for the live match. It only renders authoritative snapshots. */
public final class GameRenderer {

    private static final double BLOCK_HEIGHT_RATIO = 80.0 / 64.0;
    private static final Color GRASS_LIGHT = Color.web("#7CC04B");
    private static final Color GRASS_DARK = Color.web("#72B545");
    private static final Color OUTLINE = Color.web("#2A1F3D");
    private static final long SHAKE_NANOS = 150_000_000L;
    private static final long GO_NANOS = 900_000_000L;
    private static final long WINNER_NANOS = 1_200_000_000L;

    private final Canvas canvas;
    private final SvgAssets assets;
    private final MatchTracker tracker;
    private final BoardLayout layout;
    private final Map<String, Image> sprites = new HashMap<>();
    private final Map<Long, PlayerVisual> visuals = new HashMap<>();
    private final ParticlePool particles = new ParticlePool();
    private final Random shakeRandom = new Random(0x5A4B_2026L);
    private Image background;
    private GameStateDto snapshot;
    private List<ExplosionVisual> explosionVisuals = List.of();
    private long snapshotReceivedNanos;
    private long goStartedNanos = Long.MIN_VALUE;
    private long shakeStartedNanos = Long.MIN_VALUE;
    private Long winnerUserId;
    private long winnerStartedNanos = Long.MIN_VALUE;
    private double lastRenderMillis;

    public GameRenderer(Canvas canvas, SvgAssets assets, MatchTracker tracker) {
        this.canvas = canvas;
        this.assets = assets;
        this.tracker = tracker;
        layout = BoardLayout.fit(canvas.getWidth(), canvas.getHeight());
        rebuildBackground();
        loadSprites();
        assets.renderScaleProperty().addListener(observable -> loadSprites());
    }

    /** Starts SVG rasterisation while players are still in the Room Lobby. */
    public static void prewarm(SvgAssets assets) {
        BoardLayout layout = BoardLayout.fit(1280, 720);
        double scale = roundedScale(assets.renderScaleProperty().get());
        int tile = pixels(layout.tile(), scale);
        int blockHeight = pixels(layout.tile() * BLOCK_HEIGHT_RATIO, scale);
        List<SvgAssets.Request> requests = new ArrayList<>();
        requests.add(new SvgAssets.Request(AssetIds.BLOCK_STONE, null, tile, blockHeight));
        requests.add(new SvgAssets.Request(AssetIds.CRATE, null, tile, blockHeight));
        requests.add(new SvgAssets.Request(AssetIds.BOMB, null, tile, tile));
        requests.add(new SvgAssets.Request(AssetIds.SPARK, null, Math.max(1, tile / 2), Math.max(1, tile / 2)));
        requests.add(new SvgAssets.Request(AssetIds.FLAME_CENTER, null, tile, tile));
        requests.add(new SvgAssets.Request(AssetIds.FLAME_MID, null, tile, tile));
        requests.add(new SvgAssets.Request(AssetIds.FLAME_END, null, tile, tile));
        for (TeamColor team : TeamColor.values()) {
            for (String asset : List.of(AssetIds.BOMBER_DOWN, AssetIds.BOMBER_UP, AssetIds.BOMBER_SIDE)) {
                requests.add(new SvgAssets.Request(asset, team, tile, tile));
            }
        }
        assets.prewarm(requests);
    }

    public void acceptSnapshot(GameStateDto next, FrameEvents events, long nowNanos) {
        snapshot = next;
        explosionVisuals = next.explosions().stream()
                .map(explosion -> new ExplosionVisual(explosion, FlameClassifier.classify(explosion)))
                .toList();
        snapshotReceivedNanos = nowNanos;
        if (events.newMatch()) {
            particles.clear();
            visuals.clear();
            winnerUserId = null;
            winnerStartedNanos = Long.MIN_VALUE;
            goStartedNanos = nowNanos;
        }
        for (GamePlayerStateDto player : next.players()) {
            PlayerVisual visual = visuals.computeIfAbsent(player.userId(), ignored -> new PlayerVisual(player.position()));
            visual.setTarget(player.position());
            visual.setAlive(player.alive(), nowNanos);
        }
        for (PositionDto cell : events.brokenCrates()) {
            particles.spawnCrateDebris(layout.cellX(cell.column()), layout.cellY(cell.row()), layout.tile());
        }
        for (Long userId : events.movedPlayers()) {
            PlayerVisual visual = visuals.get(userId);
            if (visual != null) {
                particles.spawnDust(layout.cellX(visual.x() + 0.5), layout.cellY(visual.y() + 0.92), layout.tile());
            }
        }
        if (!events.newExplosions().isEmpty()) {
            shakeStartedNanos = nowNanos;
        }
    }

    public void showWinner(Long winnerUserId, long nowNanos) {
        if (winnerStartedNanos == Long.MIN_VALUE) {
            this.winnerUserId = winnerUserId;
            winnerStartedNanos = nowNanos;
        }
    }

    public void render(long nowNanos, double elapsedSeconds) {
        long started = System.nanoTime();
        GraphicsContext graphics = canvas.getGraphicsContext2D();
        graphics.setImageSmoothing(true);
        graphics.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        visuals.values().forEach(visual -> visual.advance(elapsedSeconds));
        particles.update(elapsedSeconds);

        graphics.save();
        applyShake(graphics, nowNanos);
        if (background != null) {
            graphics.drawImage(background, 0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            graphics.setFill(Color.web("#F39A38"));
            graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        if (snapshot != null) {
            drawWorld(graphics, nowNanos);
        }
        graphics.restore();
        lastRenderMillis = (System.nanoTime() - started) / 1_000_000.0;
    }

    public void reset() {
        snapshot = null;
        explosionVisuals = List.of();
        visuals.clear();
        particles.clear();
        winnerUserId = null;
        winnerStartedNanos = Long.MIN_VALUE;
        goStartedNanos = Long.MIN_VALUE;
        shakeStartedNanos = Long.MIN_VALUE;
    }

    public double lastRenderMillis() {
        return lastRenderMillis;
    }

    private void drawWorld(GraphicsContext graphics, long nowNanos) {
        drawShadows(graphics);
        for (int row = 0; row < snapshot.map().size(); row++) {
            drawBlocksInRow(graphics, row);
            drawBombsInRow(graphics, row, nowNanos);
            drawPlayersInRow(graphics, row, nowNanos);
        }
        drawExplosions(graphics, nowNanos);
        particles.draw(graphics);
        drawNames(graphics, nowNanos);
        drawGo(graphics, nowNanos);
    }

    private void drawShadows(GraphicsContext graphics) {
        graphics.setFill(Color.rgb(20, 16, 28, 0.28));
        double tile = layout.tile();
        for (BombStateDto bomb : snapshot.bombs()) {
            graphics.fillOval(layout.cellX(bomb.position().column()) + tile * 0.18,
                    layout.cellY(bomb.position().row()) + tile * 0.68, tile * 0.64, tile * 0.2);
        }
        for (GamePlayerStateDto player : snapshot.players()) {
            PlayerVisual visual = visuals.get(player.userId());
            if (visual != null) {
                graphics.fillOval(layout.cellX(visual.x()) + tile * 0.14,
                        layout.cellY(visual.y()) + tile * 0.76, tile * 0.72, tile * 0.2);
            }
        }
    }

    private void drawBlocksInRow(GraphicsContext graphics, int row) {
        if (row >= snapshot.map().size()) {
            return;
        }
        List<TileType> tiles = snapshot.map().get(row);
        for (int column = 0; column < tiles.size(); column++) {
            TileType tileType = tiles.get(column);
            if (tileType == TileType.EMPTY) {
                continue;
            }
            Image image = sprites.get(tileType == TileType.HARD_WALL ? AssetIds.BLOCK_STONE : AssetIds.CRATE);
            double x = layout.cellX(column);
            double y = layout.blockY(row);
            double height = layout.tile() * BLOCK_HEIGHT_RATIO;
            if (image != null) {
                graphics.drawImage(image, x, y, layout.tile(), height);
            } else {
                graphics.setFill(tileType == TileType.HARD_WALL ? Color.web("#9184A8") : Color.web("#B8581F"));
                graphics.fillRoundRect(x + 1, y + 1, layout.tile() - 2, height - 2, 7, 7);
            }
        }
    }

    private void drawBombsInRow(GraphicsContext graphics, int row, long nowNanos) {
        double tile = layout.tile();
        for (BombStateDto bomb : snapshot.bombs()) {
            if (bomb.position().row() != row) {
                continue;
            }
            long remaining = adjustedRemaining(bomb.remainingFuseMillis(), nowNanos);
            double progress = 1 - Math.min(1, remaining / (double) ServerRules.BOMB_FUSE_MILLIS);
            double hertz = 2 + progress * 6;
            double pulse = 1 + 0.06 * Math.sin(nowNanos / 1_000_000_000.0 * Math.PI * 2 * hertz);
            double size = tile * 0.88 * pulse;
            double x = layout.cellX(bomb.position().column()) + (tile - size) / 2;
            double y = layout.cellY(bomb.position().row()) + tile - size;
            Image bombImage = sprites.get(AssetIds.BOMB);
            if (bombImage != null) {
                graphics.drawImage(bombImage, x, y, size, size);
            } else {
                graphics.setFill(OUTLINE);
                graphics.fillOval(x, y, size, size);
            }
            if (remaining < 1_000) {
                graphics.setFill(Color.rgb(255, 50, 35, (1_000 - remaining) / 1_000.0 * 0.45));
                graphics.fillOval(x, y, size, size);
            }
            drawSpark(graphics, x + size * 0.72, y + size * 0.02, tile, nowNanos);
        }
    }

    private void drawSpark(GraphicsContext graphics, double x, double y, double tile, long nowNanos) {
        Image spark = sprites.get(AssetIds.SPARK);
        double size = tile * (0.24 + 0.05 * Math.sin(nowNanos / 1_000_000_000.0 * Math.PI * 4));
        graphics.save();
        graphics.translate(x, y);
        graphics.rotate((nowNanos / 1_000_000.0) / 600 * 360);
        if (spark != null) {
            graphics.drawImage(spark, -size / 2, -size / 2, size, size);
        } else {
            graphics.setFill(Color.web("#FFE066"));
            graphics.fillOval(-size / 2, -size / 2, size, size);
        }
        graphics.restore();
    }

    private void drawPlayersInRow(GraphicsContext graphics, int row, long nowNanos) {
        snapshot.players().stream()
                .filter(player -> {
                    PlayerVisual visual = visuals.get(player.userId());
                    return visual != null && Math.max(0, Math.min(ServerRules.MAP_ROWS - 1,
                            (int) Math.round(visual.y()))) == row;
                })
                .sorted(Comparator.comparingDouble(player -> visuals.get(player.userId()).y()))
                .forEach(player -> drawPlayer(graphics, player, nowNanos));
    }

    private void drawPlayer(GraphicsContext graphics, GamePlayerStateDto player, long nowNanos) {
        PlayerVisual visual = visuals.get(player.userId());
        if (visual == null) {
            return;
        }
        OptionalInt slotValue = tracker.slotFor(player.userId());
        int slot = slotValue.orElse(0);
        TeamColor team = TeamColor.forSlot(slot);
        String asset = switch (visual.facing()) {
            case UP -> AssetIds.BOMBER_UP;
            case LEFT, RIGHT -> AssetIds.BOMBER_SIDE;
            case DOWN -> AssetIds.BOMBER_DOWN;
        };
        Image image = sprites.get(spriteKey(asset, team));
        double tile = layout.tile();
        double width = tile * 0.94;
        double height = tile * 0.94;
        double death = visual.deathProgress(nowNanos);
        double winnerJump = winnerJump(player.userId(), nowNanos);
        double x = layout.cellX(visual.x()) + (tile - width) / 2;
        double y = layout.cellY(visual.y()) + tile - height + visual.walkBob(nowNanos) - death * 12 - winnerJump;

        graphics.save();
        graphics.setGlobalAlpha(player.alive() ? 1 : 1 - death * 0.65);
        graphics.translate(x + width / 2, y + height / 2);
        graphics.rotate(visual.walkTilt(nowNanos));
        if (visual.facing() == PlayerVisual.Facing.LEFT) {
            graphics.scale(-1, 1);
        }
        if (image != null) {
            graphics.drawImage(image, -width / 2, -height / 2, width, height);
        } else {
            graphics.setFill(team.mainColor());
            graphics.fillOval(-width * 0.36, -height * 0.45, width * 0.72, height * 0.85);
        }
        graphics.restore();

        if (!player.alive() && death < 0.34 && ((int) (death * 12)) % 2 == 0) {
            graphics.setFill(Color.rgb(255, 255, 255, 0.7));
            graphics.fillOval(x + width * 0.1, y, width * 0.8, height * 0.9);
        }
    }

    private void drawExplosions(GraphicsContext graphics, long nowNanos) {
        for (ExplosionVisual visual : explosionVisuals) {
            ExplosionStateDto explosion = visual.explosion();
            long remaining = adjustedRemaining(explosion.remainingMillis(), nowNanos);
            double age = ServerRules.EXPLOSION_MILLIS - remaining;
            double scale = age < 80 ? 0.6 + 0.5 * age / 80.0
                    : age < 150 ? 1.1 - 0.1 * (age - 80) / 70.0 : 1;
            double alpha = age < 150 ? 1 : Math.max(0, remaining / 350.0);
            for (Piece piece : visual.pieces()) {
                drawFlamePiece(graphics, piece, scale, alpha);
            }
            if (age < 80) {
                PositionDto origin = explosion.origin();
                graphics.setFill(Color.rgb(255, 255, 255, 0.8 * (1 - age / 80.0)));
                graphics.fillOval(layout.cellX(origin.column()) + layout.tile() * 0.18,
                        layout.cellY(origin.row()) + layout.tile() * 0.18,
                        layout.tile() * 0.64, layout.tile() * 0.64);
            }
        }
    }

    private void drawFlamePiece(GraphicsContext graphics, Piece piece, double scale, double alpha) {
        String asset = switch (piece.kind()) {
            case CENTER -> AssetIds.FLAME_CENTER;
            case MID -> AssetIds.FLAME_MID;
            case END -> AssetIds.FLAME_END;
        };
        Image image = sprites.get(asset);
        double size = layout.tile() * scale;
        double centerX = layout.cellX(piece.position().column()) + layout.tile() / 2;
        double centerY = layout.cellY(piece.position().row()) + layout.tile() / 2;
        graphics.save();
        graphics.setGlobalAlpha(alpha);
        graphics.translate(centerX, centerY);
        graphics.rotate(piece.rotationDegrees());
        if (image != null) {
            graphics.drawImage(image, -size / 2, -size / 2, size, size);
        } else {
            graphics.setFill(Color.web("#FF7A1A"));
            graphics.fillOval(-size / 2, -size * 0.28, size, size * 0.56);
        }
        graphics.restore();
    }

    private void drawNames(GraphicsContext graphics, long nowNanos) {
        double tile = layout.tile();
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFont(Font.font("Nunito", FontWeight.EXTRA_BOLD, Math.max(12, tile * 0.24)));
        graphics.setLineWidth(Math.max(2, tile * 0.05));
        for (GamePlayerStateDto player : snapshot.players()) {
            PlayerVisual visual = visuals.get(player.userId());
            if (visual == null) continue;
            int slot = tracker.slotFor(player.userId()).orElse(0);
            String name = tile < 40 ? "P" + (slot + 1) : player.username();
            double x = layout.cellX(visual.x()) + tile / 2;
            double y = layout.cellY(visual.y()) - tile * 0.12 - visual.deathProgress(nowNanos) * 12;
            graphics.setStroke(OUTLINE);
            graphics.strokeText(name, x, y, tile * 2.4);
            graphics.setFill(Color.WHITE);
            graphics.fillText(name, x, y, tile * 2.4);
        }
    }

    private void drawGo(GraphicsContext graphics, long nowNanos) {
        if (goStartedNanos == Long.MIN_VALUE) return;
        double progress = (nowNanos - goStartedNanos) / (double) GO_NANOS;
        if (progress < 0 || progress > 1) return;
        double scale = 0.65 + Math.min(1, progress * 5) * 0.5;
        double alpha = progress < 0.55 ? 1 : (1 - progress) / 0.45;
        graphics.save();
        graphics.setGlobalAlpha(Math.max(0, alpha));
        graphics.translate(canvas.getWidth() / 2, canvas.getHeight() / 2 + 40);
        graphics.scale(scale, scale);
        graphics.setTextAlign(TextAlignment.CENTER);
        graphics.setFont(Font.font("Lilita One", 92));
        graphics.setLineWidth(12);
        graphics.setStroke(OUTLINE);
        graphics.strokeText("GO!", 0, 0);
        graphics.setFill(Color.web("#FFE066"));
        graphics.fillText("GO!", 0, 0);
        graphics.restore();
    }

    private double winnerJump(long userId, long nowNanos) {
        if (winnerUserId == null || winnerUserId != userId || winnerStartedNanos == Long.MIN_VALUE) return 0;
        double progress = (nowNanos - winnerStartedNanos) / (double) WINNER_NANOS;
        if (progress < 0 || progress > 1) return 0;
        return Math.abs(Math.sin(progress * Math.PI * 2)) * layout.tile() * 0.22;
    }

    private long adjustedRemaining(long serverRemaining, long nowNanos) {
        long elapsedMillis = Math.max(0, (nowNanos - snapshotReceivedNanos) / 1_000_000L);
        return Math.max(0, serverRemaining - elapsedMillis);
    }

    private void applyShake(GraphicsContext graphics, long nowNanos) {
        if (shakeStartedNanos == Long.MIN_VALUE) return;
        double progress = (nowNanos - shakeStartedNanos) / (double) SHAKE_NANOS;
        if (progress < 0 || progress >= 1) return;
        double strength = 3 * (1 - progress);
        graphics.translate((shakeRandom.nextDouble() * 2 - 1) * strength,
                (shakeRandom.nextDouble() * 2 - 1) * strength);
    }

    private void rebuildBackground() {
        Canvas buffer = new Canvas(canvas.getWidth(), canvas.getHeight());
        GraphicsContext graphics = buffer.getGraphicsContext2D();
        graphics.setFill(Color.web("#E8852C"));
        graphics.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawDecorations(graphics);
        graphics.setFill(OUTLINE);
        graphics.fillRoundRect(layout.left() - 6, layout.top() - 6,
                layout.boardWidth() + 12, layout.boardHeight() + 12, 14, 14);
        for (int row = 0; row < ServerRules.MAP_ROWS; row++) {
            for (int column = 0; column < ServerRules.MAP_COLUMNS; column++) {
                graphics.setFill((row + column) % 2 == 0 ? GRASS_LIGHT : GRASS_DARK);
                graphics.fillRect(layout.cellX(column), layout.cellY(row), layout.tile(), layout.tile());
            }
        }
        background = buffer.snapshot(new SnapshotParameters(),
                new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight()));
    }

    private void drawDecorations(GraphicsContext graphics) {
        Random random = new Random(0xA83E_2026L);
        double side = Math.max(0, layout.left() - 20);
        for (int i = 0; i < 26; i++) {
            boolean left = i % 2 == 0;
            double x = left ? 24 + random.nextDouble() * Math.max(10, side - 52)
                    : layout.left() + layout.boardWidth() + 28 + random.nextDouble() * Math.max(10, side - 52);
            double y = 120 + random.nextDouble() * 550;
            double size = 6 + random.nextDouble() * 14;
            graphics.setFill(i % 3 == 0 ? Color.web("#FFE066") : Color.web("#5AAE42"));
            graphics.fillOval(x, y, size, size * (0.65 + random.nextDouble() * 0.4));
            if (i % 5 == 0) {
                graphics.setFill(Color.web("#9C4718"));
                graphics.fillRoundRect(x - size * 0.7, y + size * 0.65, size * 2.2, size * 0.7, 6, 6);
            }
        }
    }

    private void loadSprites() {
        double scale = roundedScale(assets.renderScaleProperty().get());
        double tile = layout.tile();
        request(AssetIds.BLOCK_STONE, null, tile, tile * BLOCK_HEIGHT_RATIO, scale);
        request(AssetIds.CRATE, null, tile, tile * BLOCK_HEIGHT_RATIO, scale);
        request(AssetIds.BOMB, null, tile, tile, scale);
        request(AssetIds.SPARK, null, tile / 2, tile / 2, scale);
        request(AssetIds.FLAME_CENTER, null, tile, tile, scale);
        request(AssetIds.FLAME_MID, null, tile, tile, scale);
        request(AssetIds.FLAME_END, null, tile, tile, scale);
        for (TeamColor team : TeamColor.values()) {
            request(AssetIds.BOMBER_DOWN, team, tile, tile, scale);
            request(AssetIds.BOMBER_UP, team, tile, tile, scale);
            request(AssetIds.BOMBER_SIDE, team, tile, tile, scale);
        }
    }

    private void request(String asset, TeamColor team, double width, double height, double scale) {
        String key = team == null ? asset : spriteKey(asset, team);
        assets.request(asset, team, pixels(width, scale), pixels(height, scale))
                .thenAccept(image -> Platform.runLater(() -> sprites.put(key, image)));
    }

    private static String spriteKey(String asset, TeamColor team) {
        return asset + ':' + team.name();
    }

    private static int pixels(double logical, double scale) {
        return Math.max(1, (int) Math.ceil(logical * scale));
    }

    private static double roundedScale(double scale) {
        return Math.max(0.25, Math.ceil(scale * 4) / 4);
    }

    private record ExplosionVisual(ExplosionStateDto explosion, List<Piece> pieces) {
    }
}
