package com.bomberman.clientfx.asset;

import com.bomberman.clientfx.ui.theme.TeamColor;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Turns an asset into a bitmap of an exact pixel size using Java2D. Falls back from
 * {@code <name>.svg} to {@code <name>.png} (per-team {@code <name>_<team>.png} for
 * recoloured assets) and finally to a checkered placeholder, logging each missing asset once.
 */
public final class SvgRasterizer {

    public enum Source {
        SVG,
        PNG,
        PLACEHOLDER
    }

    public record Result(BufferedImage image, Source source) {
    }

    private static final System.Logger LOG = System.getLogger(SvgRasterizer.class.getName());
    private static final String ROOT = "/assets/svg/";

    private final Function<String, byte[]> resources;
    private final Set<String> reportedProblems = ConcurrentHashMap.newKeySet();

    /** @param resources returns the bytes of a path such as {@code /assets/svg/fx/bomb.svg}, or null */
    public SvgRasterizer(Function<String, byte[]> resources) {
        this.resources = Objects.requireNonNull(resources, "resources must not be null");
    }

    public static SvgRasterizer fromClasspath() {
        return new SvgRasterizer(SvgRasterizer::readClasspath);
    }

    /** @param team recolours character art; {@code null} renders the file as drawn */
    public Result render(String asset, TeamColor team, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Size must be positive: " + width + "x" + height);
        }
        byte[] svg = resources.apply(ROOT + asset + ".svg");
        if (svg != null) {
            BufferedImage image = renderSvg(asset, recolor(svg, team), width, height);
            if (image != null) {
                return new Result(image, Source.SVG);
            }
        } else {
            String pngName = team == null ? asset + ".png" : asset + "_" + team.fileSuffix() + ".png";
            byte[] png = resources.apply(ROOT + pngName);
            BufferedImage image = png == null ? null : renderPng(asset, png, width, height);
            if (image != null) {
                return new Result(image, Source.PNG);
            }
            reportOnce(asset, "Missing asset " + asset + " (looked for .svg and " + pngName + ")", null);
        }
        return new Result(placeholder(asset, width, height), Source.PLACEHOLDER);
    }

    private byte[] recolor(byte[] svg, TeamColor team) {
        if (team == null || team == TeamColor.RED) {
            return svg;
        }
        String recoloured = SvgRecolor.recolor(new String(svg, StandardCharsets.UTF_8), team);
        return recoloured.getBytes(StandardCharsets.UTF_8);
    }

    private BufferedImage renderSvg(String asset, byte[] svg, int width, int height) {
        try {
            SVGDocument document = new SVGLoader().load(
                    new ByteArrayInputStream(svg), null, LoaderContext.createDefault()
            );
            if (document == null) {
                reportOnce(asset, "Cannot parse asset " + asset + ".svg", null);
                return null;
            }
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = image.createGraphics();
            try {
                applyQualityHints(graphics);
                document.render((Component) null, graphics, new ViewBox(0, 0, width, height));
            } finally {
                graphics.dispose();
            }
            return image;
        } catch (RuntimeException exception) {
            reportOnce(asset, "Cannot render asset " + asset + ".svg", exception);
            return null;
        }
    }

    private BufferedImage renderPng(String asset, byte[] png, int width, int height) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(png));
            if (source == null) {
                reportOnce(asset, "Cannot decode asset " + asset + ".png", null);
                return null;
            }
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = image.createGraphics();
            try {
                applyQualityHints(graphics);
                graphics.drawImage(source, 0, 0, width, height, null);
            } finally {
                graphics.dispose();
            }
            return image;
        } catch (IOException exception) {
            reportOnce(asset, "Cannot decode asset " + asset + ".png", exception);
            return null;
        }
    }

    private static BufferedImage placeholder(String asset, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = image.createGraphics();
        try {
            int cell = Math.max(4, Math.min(width, height) / 6);
            for (int y = 0; y < height; y += cell) {
                for (int x = 0; x < width; x += cell) {
                    boolean dark = ((x / cell) + (y / cell)) % 2 == 0;
                    graphics.setColor(dark ? new Color(0xE0, 0x3E, 0xC8) : new Color(0x2A, 0x1F, 0x3D));
                    graphics.fillRect(x, y, cell, cell);
                }
            }
            graphics.setColor(Color.WHITE);
            graphics.setStroke(new BasicStroke(Math.max(1f, width / 40f)));
            graphics.drawRect(0, 0, width - 1, height - 1);
            if (width >= 48) {
                applyQualityHints(graphics);
                String label = asset.substring(asset.lastIndexOf('/') + 1);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(9, width / 10)));
                int textWidth = graphics.getFontMetrics().stringWidth(label);
                graphics.drawString(label, Math.max(2, (width - textWidth) / 2), height / 2);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void applyQualityHints(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private void reportOnce(String asset, String message, Throwable cause) {
        if (reportedProblems.add(asset)) {
            LOG.log(System.Logger.Level.WARNING, message, cause);
        }
    }

    private static byte[] readClasspath(String path) {
        try (InputStream input = SvgRasterizer.class.getResourceAsStream(path)) {
            return input == null ? null : input.readAllBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read " + path, exception);
        }
    }
}
