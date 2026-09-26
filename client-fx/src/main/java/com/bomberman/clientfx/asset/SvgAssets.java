package com.bomberman.clientfx.asset;

import com.bomberman.clientfx.ui.theme.TeamColor;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous, size-keyed cache of rasterised assets. Rendering runs on background threads;
 * callers attach the finished {@link Image} on the JavaFX thread.
 */
public final class SvgAssets implements AutoCloseable {

    /** Pixel size request; {@code team == null} renders the file unchanged. */
    public record Request(String asset, TeamColor team, int pixelWidth, int pixelHeight) {

        public Request {
            Objects.requireNonNull(asset, "asset must not be null");
            if (pixelWidth <= 0 || pixelHeight <= 0) {
                throw new IllegalArgumentException("Size must be positive");
            }
        }

        long bytes() {
            return (long) pixelWidth * pixelHeight * Integer.BYTES;
        }
    }

    private static final long CACHE_BUDGET_BYTES = 64L * 1024 * 1024;

    private final SvgRasterizer rasterizer;
    private final ExecutorService workers = Executors.newFixedThreadPool(2, Thread.ofPlatform()
            .name("asset-renderer-", 0)
            .daemon()
            .factory());
    private final Map<Request, CompletableFuture<Image>> cache = new LinkedHashMap<>(64, 0.75f, true);
    private final DoubleProperty renderScale = new SimpleDoubleProperty(this, "renderScale", 1);
    private long cachedBytes;

    public SvgAssets(SvgRasterizer rasterizer) {
        this.rasterizer = Objects.requireNonNull(rasterizer, "rasterizer must not be null");
    }

    /**
     * Device pixels per logical pixel for UI images (design-frame scale × screen output scale).
     * Views re-request their images when it changes.
     */
    public DoubleProperty renderScaleProperty() {
        return renderScale;
    }

    public CompletableFuture<Image> request(Request request) {
        synchronized (cache) {
            CompletableFuture<Image> cached = cache.get(request);
            if (cached != null) {
                return cached;
            }
            CompletableFuture<Image> image = CompletableFuture.supplyAsync(() -> toFxImage(
                    rasterizer.render(request.asset(), request.team(), request.pixelWidth(), request.pixelHeight())
                            .image()
            ), workers);
            cache.put(request, image);
            cachedBytes += request.bytes();
            evictOverBudget();
            return image;
        }
    }

    public CompletableFuture<Image> request(String asset, TeamColor team, int pixelWidth, int pixelHeight) {
        return request(new Request(asset, team, pixelWidth, pixelHeight));
    }

    /** Starts rendering images that will be needed soon, e.g. game sprites while in the room lobby. */
    public void prewarm(List<Request> requests) {
        requests.forEach(this::request);
    }

    private void evictOverBudget() {
        Iterator<Map.Entry<Request, CompletableFuture<Image>>> eldestFirst = cache.entrySet().iterator();
        while (cachedBytes > CACHE_BUDGET_BYTES && cache.size() > 1 && eldestFirst.hasNext()) {
            cachedBytes -= eldestFirst.next().getKey().bytes();
            eldestFirst.remove();
        }
    }

    static Image toFxImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        WritableImage result = new WritableImage(width, height);
        result.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
        return result;
    }

    @Override
    public void close() {
        workers.shutdownNow();
    }
}
