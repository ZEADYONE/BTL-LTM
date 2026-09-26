package com.bomberman.clientfx.asset;

import com.bomberman.clientfx.ui.theme.TeamColor;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgRasterizerTest {

    private static final String SQUARE =
            "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" fill=\"#E53935\"/></svg>";

    private final Map<String, byte[]> files = new HashMap<>();
    private final List<String> requestedPaths = new ArrayList<>();
    private final SvgRasterizer rasterizer = new SvgRasterizer(path -> {
        requestedPaths.add(path);
        return files.get(path);
    });

    @Test
    void rendersSvgAtTheRequestedPixelSize() {
        files.put("/assets/svg/test/square.svg", SQUARE.getBytes(StandardCharsets.UTF_8));

        SvgRasterizer.Result result = rasterizer.render("test/square", null, 40, 24);

        assertEquals(SvgRasterizer.Source.SVG, result.source());
        assertEquals(40, result.image().getWidth());
        assertEquals(24, result.image().getHeight());
        assertColour(0xE53935, result.image().getRGB(20, 12));
    }

    @Test
    void recolouredCharactersUseTheTeamPalette() {
        files.put("/assets/svg/test/square.svg", SQUARE.getBytes(StandardCharsets.UTF_8));

        BufferedImage blue = rasterizer.render("test/square", TeamColor.BLUE, 16, 16).image();

        assertColour(0x1E88E5, blue.getRGB(8, 8));
    }

    @Test
    void pngWithTeamSuffixIsTheFallbackForMissingSvg() throws IOException {
        files.put("/assets/svg/test/hero_green.png", png(new Color(0x43A047)));

        SvgRasterizer.Result result = rasterizer.render("test/hero", TeamColor.GREEN, 32, 32);

        assertEquals(SvgRasterizer.Source.PNG, result.source());
        assertColour(0x43A047, result.image().getRGB(16, 16));
        assertTrue(requestedPaths.contains("/assets/svg/test/hero.svg"));
    }

    @Test
    void missingOrBrokenAssetsBecomeAPlaceholderOfTheSameSize() {
        files.put("/assets/svg/test/broken.svg", "<svg><not closed".getBytes(StandardCharsets.UTF_8));

        SvgRasterizer.Result missing = rasterizer.render("test/nothing", null, 64, 48);
        SvgRasterizer.Result broken = rasterizer.render("test/broken", null, 20, 20);

        assertEquals(SvgRasterizer.Source.PLACEHOLDER, missing.source());
        assertEquals(64, missing.image().getWidth());
        assertEquals(48, missing.image().getHeight());
        assertEquals(SvgRasterizer.Source.PLACEHOLDER, broken.source());
    }

    @Test
    void everyBundledAssetParsesAndDrawsSomething() throws ReflectiveOperationException {
        SvgRasterizer classpath = SvgRasterizer.fromClasspath();
        for (Field field : AssetIds.class.getFields()) {
            String asset = (String) field.get(null);
            SvgRasterizer.Result result = classpath.render(asset, TeamColor.GREEN, 64, 64);
            assertEquals(SvgRasterizer.Source.SVG, result.source(), asset);
            assertTrue(hasVisiblePixel(result.image()), asset + " rendered fully transparent");
        }
    }

    @Test
    void characterArtOnlyUsesKeyColoursForTeamParts() throws ReflectiveOperationException {
        SvgRasterizer classpath = SvgRasterizer.fromClasspath();
        for (String asset : List.of(AssetIds.BOMBER_FULL, AssetIds.BOMBER_HEAD, AssetIds.BOMBER_DOWN,
                AssetIds.BOMBER_UP, AssetIds.BOMBER_SIDE)) {
            BufferedImage red = classpath.render(asset, TeamColor.RED, 96, 96).image();
            BufferedImage blue = classpath.render(asset, TeamColor.BLUE, 96, 96).image();
            assertTrue(differs(red, blue), asset + " did not change colour for another team");
        }
    }

    private static boolean hasVisiblePixel(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean differs(BufferedImage first, BufferedImage second) {
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] png(Color colour) throws IOException {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                image.setRGB(x, y, colour.getRGB());
            }
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private static void assertColour(int expectedRgb, int argb) {
        assertEquals(0xFF, argb >>> 24, "pixel should be opaque");
        assertEquals(expectedRgb, argb & 0xFFFFFF, String.format("expected %06X but was %06X", expectedRgb, argb & 0xFFFFFF));
    }
}
