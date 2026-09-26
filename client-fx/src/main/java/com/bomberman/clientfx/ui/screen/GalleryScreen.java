package com.bomberman.clientfx.ui.screen;

import com.bomberman.clientfx.asset.AssetIds;
import com.bomberman.clientfx.asset.SvgAssets;
import com.bomberman.clientfx.state.Feedback;
import com.bomberman.clientfx.ui.AppShell;
import com.bomberman.clientfx.ui.Screen;
import com.bomberman.clientfx.ui.component.ChecklistItem;
import com.bomberman.clientfx.ui.component.Confetti;
import com.bomberman.clientfx.ui.component.GameButton;
import com.bomberman.clientfx.ui.component.GameButton.Size;
import com.bomberman.clientfx.ui.component.GameButton.Tone;
import com.bomberman.clientfx.ui.component.GameFields;
import com.bomberman.clientfx.ui.component.GameTabs;
import com.bomberman.clientfx.ui.component.HudPlayerCard;
import com.bomberman.clientfx.ui.component.MenuTileButton;
import com.bomberman.clientfx.ui.component.OutlinedText;
import com.bomberman.clientfx.ui.component.Panel;
import com.bomberman.clientfx.ui.component.Pill;
import com.bomberman.clientfx.ui.component.PlayerBadge;
import com.bomberman.clientfx.ui.component.PlayerHead;
import com.bomberman.clientfx.ui.component.RankRow;
import com.bomberman.clientfx.ui.component.RoomCard;
import com.bomberman.clientfx.ui.component.SlotCard;
import com.bomberman.clientfx.ui.component.SunRays;
import com.bomberman.clientfx.ui.component.SvgView;
import com.bomberman.clientfx.ui.component.Tag;
import com.bomberman.clientfx.ui.component.TitleBanner;
import com.bomberman.clientfx.ui.theme.TeamColor;
import com.bomberman.common.enums.RoomStatus;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Developer screen ({@code --gallery}) showing every component in every state and every asset,
 * used to review the design system and newly delivered SVGs (docs/ui-redesign/05, section 9).
 */
public final class GalleryScreen implements Screen {

    private static final double TILE = 56;

    private final SvgAssets assets;
    private final AppShell shell;
    private final StackPane root = new StackPane();
    private final Confetti confetti = new Confetti();
    private final SunRays rays = new SunRays(170);
    private boolean spinning;

    public GalleryScreen(SvgAssets assets, AppShell shell) {
        this.assets = assets;
        this.shell = shell;

        VBox content = new VBox(28,
                TitleBanner.burst("GALLERY"),
                section("Buttons", buttons()),
                section("Menu tiles", menuTiles()),
                section("Titles", titles()),
                section("Panels, pills, badge, tags", panels()),
                section("Inputs, tabs, checklist", inputs()),
                section("Characters (4 team colours)", characters()),
                section("Room Lobby slots", slots()),
                section("Room Browser cards", roomCards()),
                section("Result / Leaderboard rows", rankRows()),
                section("Match HUD", hudCards()),
                section("Tiles, bomb and blast", board()),
                section("Icons", icons()),
                section("Missing asset → placeholder", new SvgView(assets, "icons/does_not_exist", 96, 96)),
                section("Effects", effects())
        );
        content.setPadding(new Insets(24, 32, 40, 32));
        content.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(content);
        scroll.getStyleClass().add("game-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            double page = scroll.getViewportBounds().getHeight() / Math.max(1, content.getHeight() - scroll.getViewportBounds().getHeight());
            switch (event.getCode()) {
                case PAGE_DOWN -> scroll.setVvalue(Math.min(1, scroll.getVvalue() + page));
                case PAGE_UP -> scroll.setVvalue(Math.max(0, scroll.getVvalue() - page));
                case HOME -> scroll.setVvalue(0);
                case END -> scroll.setVvalue(1);
                default -> {
                    return;
                }
            }
            event.consume();
        });
        confetti.prefWidthProperty().bind(root.widthProperty());
        confetti.prefHeightProperty().bind(root.heightProperty());
        root.getChildren().addAll(scroll, confetti);
    }

    @Override
    public Node root() {
        return root;
    }

    @Override
    public void onHide() {
        rays.stop();
    }

    private Node buttons() {
        VBox rows = new VBox(14);
        for (Tone tone : Tone.values()) {
            rows.getChildren().add(row(
                    new GameButton(tone.name(), tone, Size.L),
                    new GameButton("MEDIUM", tone, Size.M),
                    new GameButton("SMALL", tone, Size.S),
                    new GameButton("", tone, Size.ICON, icon("mdi2c-close-thick", 26))
            ));
        }
        GameButton disabled = new GameButton("DISABLED", Tone.GREEN, Size.M);
        disabled.setDisable(true);
        GameButton loading = new GameButton("LOADING", Tone.YELLOW, Size.M);
        loading.setOnAction(event -> loading.setLoading(true));
        GameButton reset = new GameButton("RESET", Tone.BLUE, Size.S, icon("mdi2r-refresh", 20));
        reset.setOnAction(event -> loading.setLoading(false));
        rows.getChildren().addAll(
                row(new GameButton("PLAY", Tone.YELLOW, Size.XL, new SvgView(assets, AssetIds.BOMB, 72, 72)),
                        new GameButton("START GAME", Tone.YELLOW, Size.XL)),
                row(disabled, loading, reset,
                        new GameButton("BACK", Tone.BLUE, Size.L, icon("mdi2a-arrow-left-bold", 30)),
                        new GameButton("HOME", Tone.BLUE, Size.L, icon("mdi2h-home", 30)))
        );
        return rows;
    }

    private Node menuTiles() {
        VBox column = new VBox(26,
                new MenuTileButton("LEADERBOARD", assets, AssetIds.ICON_TROPHY),
                new MenuTileButton("HISTORY", assets, AssetIds.ICON_HISTORY),
                new MenuTileButton("SETTINGS", assets, AssetIds.ICON_GEAR),
                new MenuTileButton("HELP", assets, AssetIds.ICON_HELP)
        );
        column.setPadding(new Insets(10, 0, 0, 20));
        return column;
    }

    private Node titles() {
        return new VBox(18,
                row(TitleBanner.purple("ROOM LOBBY", new SvgView(assets, AssetIds.ICON_PLAYERS, 56, 56)),
                        TitleBanner.burst("SELECT MODE")),
                row(new OutlinedText("VICTORY", OutlinedText.Style.DISPLAY, OutlinedText.GOLD),
                        new OutlinedText("DEFEAT", OutlinedText.Style.DISPLAY, OutlinedText.STEEL),
                        new OutlinedText("DRAW", OutlinedText.Style.DISPLAY, OutlinedText.SILVER))
        );
    }

    private Node panels() {
        Label description = new Label("The classic bomber!\nBalanced and reliable in any situation.\nTên có dấu: Nguyễn Văn Đức");
        description.getStyleClass().add("label-body");
        description.setWrapText(true);
        Panel cream = new Panel(Panel.Style.CREAM, "RED BOMBER", description);
        cream.setMinWidth(380);

        Label info = new Label("GAME MODE  CLASSIC\nWIN  LAST ONE STANDING\nPLAYERS  2/4");
        info.getStyleClass().add("label-display");
        Panel brown = new Panel(Panel.Style.BROWN, "INFO", info);
        brown.setMinWidth(280);
        Label purpleText = new Label("Map preview goes here");
        purpleText.getStyleClass().add("label-body");
        Panel purple = new Panel(Panel.Style.PURPLE, "MAP", purpleText);
        purple.setMinWidth(240);

        PlayerBadge badge = new PlayerBadge(assets);
        badge.setPlayer("Minh Đức", TeamColor.RED, "RANK #3 · 12.5 PTS");
        VBox pills = new VBox(12,
                new Pill(new SvgView(assets, AssetIds.ICON_STAR, 32, 32), "12.5"),
                new Pill(icon("mdi2c-circle", 20), "5 ONLINE"),
                badge,
                row(new Tag("HOST", Tag.Kind.HOST, new SvgView(assets, AssetIds.ICON_CROWN, 22, 22)),
                        new Tag("YOU", Tag.Kind.YOU), new Tag("WAITING", Tag.Kind.WAITING),
                        new Tag("PLAYING", Tag.Kind.PLAYING), new Tag("FINISHED", Tag.Kind.FINISHED),
                        new Tag("WINNER", Tag.Kind.WINNER), new Tag("DRAW", Tag.Kind.DRAW))
        );
        return new VBox(16, row(cream, purple, brown), pills);
    }

    private Node inputs() {
        TextField username = GameFields.text("Username");
        PasswordField password = GameFields.password("Password");
        TextField invalid = GameFields.text("Room name");
        invalid.setText("   ");
        GameFields.markError(invalid, true);
        Label error = new Label("Room name must be 1–60 characters.");
        error.getStyleClass().add("field-error");
        VBox fields = new VBox(10, new GameTabs("LOGIN", "REGISTER"), username, password, invalid, error);
        fields.setPrefWidth(360);

        VBox checklist = new VBox(10,
                new ChecklistItem(ChecklistItem.State.DONE, "Players 2/4 (min 2)"),
                new ChecklistItem(ChecklistItem.State.NOT_DONE, "All players ready (1/2)"),
                new ChecklistItem(ChecklistItem.State.WAITING, "Waiting for players…")
        );
        Panel status = new Panel(Panel.Style.PURPLE, "ROOM STATUS", checklist);
        status.setPrefWidth(420);
        return row(fields, status);
    }

    private Node characters() {
        HBox full = new HBox(12);
        HBox heads = new HBox(12);
        GridPane sprites = new GridPane();
        sprites.setHgap(10);
        sprites.setVgap(6);
        for (TeamColor team : TeamColor.values()) {
            full.getChildren().add(new SvgView(assets, AssetIds.BOMBER_FULL, team, 170, 170));
            heads.getChildren().add(new PlayerHead(assets, team, 64, true));
            int column = team.ordinal() * 4;
            sprites.add(new SvgView(assets, AssetIds.BOMBER_DOWN, team, 64, 64), column, 0);
            sprites.add(new SvgView(assets, AssetIds.BOMBER_UP, team, 64, 64), column + 1, 0);
            sprites.add(new SvgView(assets, AssetIds.BOMBER_SIDE, team, 64, 64), column + 2, 0);
            SvgView left = new SvgView(assets, AssetIds.BOMBER_SIDE, team, 64, 64);
            left.setScaleX(-1);
            sprites.add(left, column + 3, 0);
        }
        PlayerHead empty = new PlayerHead(assets, TeamColor.BLUE, 64, true);
        empty.setState(PlayerHead.State.EMPTY);
        PlayerHead out = new PlayerHead(assets, TeamColor.GREEN, 64, true);
        out.setState(PlayerHead.State.OUT);
        heads.getChildren().addAll(empty, out);
        return new VBox(12, full, heads, sprites);
    }

    private Node slots() {
        SlotCard host = new SlotCard(assets, 0);
        host.showPlayer("PLAYER", true, true, false);
        SlotCard you = new SlotCard(assets, 1);
        you.showPlayer("Minh Đức", false, false, true);
        SlotCard third = new SlotCard(assets, 2);
        third.showPlayer("Momo", true, false, false);
        SlotCard empty = new SlotCard(assets, 3);
        FlowPane grid = new FlowPane(14, 14, host, you, third, empty);
        grid.setPrefWrapLength(SlotCard.WIDTH * 2 + 14);
        return grid;
    }

    private Node roomCards() {
        RoomCard waiting = new RoomCard(assets);
        waiting.update("Bomber Party!", "alex", 2, 4, RoomStatus.WAITING);
        RoomCard playing = new RoomCard(assets);
        playing.update("Phòng của Đức", "Minh Đức", 3, 4, RoomStatus.PLAYING);
        RoomCard full = new RoomCard(assets);
        full.update("Full house", null, 4, 4, RoomStatus.WAITING);
        List.of(waiting, playing, full).forEach(card -> card.setPrefWidth(300));
        return row(waiting, playing, full);
    }

    private Node rankRows() {
        VBox rows = new VBox(10,
                new RankRow(assets, 1, "PLAYER", TeamColor.RED, "+1", new Tag("WINNER", Tag.Kind.WINNER), true),
                new RankRow(assets, 2, "BLUE", TeamColor.BLUE, "+0", null, false),
                new RankRow(assets, 3, "Nguyễn Văn Đức", TeamColor.GREEN, "+0", null, false),
                new RankRow(assets, 4, "YELLOW", TeamColor.YELLOW, "+0", null, false)
        );
        rows.setMaxWidth(560);
        return rows;
    }

    private Node hudCards() {
        HudPlayerCard alive = new HudPlayerCard(assets, 0, "alex");
        alive.update(1, 2, true);
        HudPlayerCard out = new HudPlayerCard(assets, 1, "Minh Đức");
        out.update(0, 2, false);
        HudPlayerCard third = new HudPlayerCard(assets, 2, "momo");
        third.update(1, 2, true);
        HudPlayerCard fourth = new HudPlayerCard(assets, 3, "tako");
        fourth.update(0, 2, true);
        return row(alive, out, third, fourth);
    }

    /** A 7×5 patch of arena showing the 3/4 block overlap, a bomb and an assembled blast. */
    private Node board() {
        String[] layout = {
                "#######",
                "#..c..#",
                "#.#b#.#",
                "#.....#",
                "#######"
        };
        Pane arena = new Pane();
        arena.setPrefSize(TILE * 7, TILE * 5.25);
        double top = TILE * 0.25;
        for (int row = 0; row < layout.length; row++) {
            for (int column = 0; column < layout[row].length(); column++) {
                Rectangle floor = new Rectangle(TILE, TILE,
                        (row + column) % 2 == 0 ? Color.web("#7CC04B") : Color.web("#72B545"));
                floor.relocate(column * TILE, top + row * TILE);
                arena.getChildren().add(floor);
            }
        }
        for (int row = 0; row < layout.length; row++) {
            for (int column = 0; column < layout[row].length(); column++) {
                char cell = layout[row].charAt(column);
                if (cell == '#' || cell == 'c') {
                    SvgView block = new SvgView(assets, cell == '#' ? AssetIds.BLOCK_STONE : AssetIds.CRATE,
                            TILE, TILE * 80 / 64);
                    block.relocate(column * TILE, top + row * TILE - TILE * 16 / 64);
                    arena.getChildren().add(block);
                } else if (cell == 'b') {
                    SvgView bomb = new SvgView(assets, AssetIds.BOMB, TILE, TILE);
                    bomb.relocate(column * TILE, top + row * TILE);
                    SvgView spark = new SvgView(assets, AssetIds.SPARK, TILE * 0.4, TILE * 0.4);
                    spark.relocate(column * TILE + TILE * 46 / 64 - TILE * 0.2, top + row * TILE + TILE * 8 / 64 - TILE * 0.2);
                    arena.getChildren().addAll(bomb, spark);
                }
            }
        }
        SvgView player = new SvgView(assets, AssetIds.BOMBER_DOWN, TeamColor.BLUE, TILE * 1.15, TILE * 1.15);
        player.relocate(TILE * 5 - TILE * 0.075, top + TILE * 3 - TILE * 0.3);
        arena.getChildren().add(player);

        Pane blast = new Pane();
        blast.setPrefSize(TILE * 5, TILE * 5);
        placeFlame(blast, AssetIds.FLAME_CENTER, 2, 2, 0);
        placeFlame(blast, AssetIds.FLAME_MID, 3, 2, 0);
        placeFlame(blast, AssetIds.FLAME_END, 4, 2, 0);
        placeFlame(blast, AssetIds.FLAME_MID, 2, 3, 90);
        placeFlame(blast, AssetIds.FLAME_END, 2, 4, 90);
        placeFlame(blast, AssetIds.FLAME_MID, 1, 2, 180);
        placeFlame(blast, AssetIds.FLAME_END, 0, 2, 180);
        placeFlame(blast, AssetIds.FLAME_END, 2, 1, 270);
        return row(arena, blast);
    }

    private void placeFlame(Pane blast, String asset, int column, int row, double rotation) {
        SvgView flame = new SvgView(assets, asset, TILE, TILE);
        flame.relocate(column * TILE, row * TILE);
        flame.setRotate(rotation);
        blast.getChildren().add(flame);
    }

    private Node icons() {
        FlowPane grid = new FlowPane(18, 14);
        List.of(AssetIds.ICON_TROPHY, AssetIds.ICON_HISTORY, AssetIds.ICON_GEAR, AssetIds.ICON_HELP,
                        AssetIds.ICON_CROWN, AssetIds.ICON_PLAYERS, AssetIds.ICON_STAR, AssetIds.ICON_FIRE,
                        AssetIds.ICON_TIMER, AssetIds.ICON_SKULL, AssetIds.BOMB, AssetIds.SPARK)
                .forEach(asset -> {
                    Label name = new Label(asset.substring(asset.indexOf('/') + 1));
                    name.getStyleClass().add("label-caption");
                    name.setStyle("-fx-text-fill: -color-outline;");
                    VBox cell = new VBox(4, new SvgView(assets, asset, 72, 72), name);
                    cell.setAlignment(Pos.CENTER);
                    grid.getChildren().add(cell);
                });
        return grid;
    }

    private Node effects() {
        GameButton info = new GameButton("TOAST INFO", Tone.PURPLE, Size.S);
        info.setOnAction(event -> shell.showToast(Feedback.info("Room is ready for a rematch")));
        GameButton success = new GameButton("TOAST OK", Tone.GREEN, Size.S);
        success.setOnAction(event -> shell.showToast(Feedback.success("Account created. You can log in now.")));
        GameButton error = new GameButton("TOAST ERROR", Tone.RED, Size.S);
        error.setOnAction(event -> shell.showToast(Feedback.error("Wrong username or password.")));
        GameButton modal = new GameButton("OPEN POPUP", Tone.BLUE, Size.S);
        modal.setOnAction(event -> shell.showModal(samplePopup(), true));
        GameButton burst = new GameButton("CONFETTI", Tone.YELLOW, Size.S);
        burst.setOnAction(event -> confetti.burst());
        // Spinning is off by default: any animation inside a ScrollPane repaints the whole viewport.
        GameButton spin = new GameButton("SPIN RAYS", Tone.GREY, Size.S);
        spin.setOnAction(event -> {
            spinning = !spinning;
            if (spinning) {
                rays.play();
            } else {
                rays.stop();
            }
        });

        StackPane hero = new StackPane(rays, new SvgView(assets, AssetIds.BOMBER_FULL, TeamColor.YELLOW, 220, 220));
        hero.setPrefSize(340, 260);
        return row(new VBox(10, info, success, error, modal, burst, spin), hero);
    }

    private Node samplePopup() {
        Label text = new Label("Leave this room?");
        text.getStyleClass().add("label-body");
        GameButton cancel = new GameButton("CANCEL", Tone.BLUE, Size.M);
        cancel.setOnAction(event -> shell.closeModal());
        GameButton leave = new GameButton("LEAVE", Tone.RED, Size.M);
        leave.setOnAction(event -> shell.closeModal());
        HBox buttons = new HBox(12, cancel, leave);
        buttons.setAlignment(Pos.CENTER);
        Panel panel = new Panel(Panel.Style.CREAM, "CONFIRM", text, buttons);
        panel.setMaxSize(420, Panel.USE_PREF_SIZE);
        return panel;
    }

    private Node section(String title, Node body) {
        Label heading = new Label(title.toUpperCase());
        heading.getStyleClass().add("label-display");
        heading.setStyle("-fx-font-size: 24px; -fx-text-fill: -color-outline;");
        VBox section = new VBox(12, heading, body);
        section.setMaxWidth(Double.MAX_VALUE);
        return section;
    }

    private static HBox row(Node... nodes) {
        HBox row = new HBox(14, nodes);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static FontIcon icon(String literal, int size) {
        FontIcon icon = new FontIcon(literal);
        icon.setIconSize(size);
        return icon;
    }
}
