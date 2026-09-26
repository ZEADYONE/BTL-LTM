package com.bomberman.clientfx.network;

import com.bomberman.common.message.NetworkMessage;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Requests waiting for the server's reply, matched by {@code requestId}. Replies and timeouts
 * complete the returned futures on the UI thread, so callers can update controls directly.
 */
public final class PendingRequests implements AutoCloseable {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private record Entry(CompletableFuture<NetworkMessage> reply, boolean quietErrors, ScheduledFuture<?> timeout) {
    }

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Executor uiThread;
    private final Duration timeout;
    private final Runnable onTimeout;
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().name("request-timeout").daemon().factory());

    /** @param onTimeout runs on the UI thread whenever a request expires without a reply */
    public PendingRequests(Executor uiThread, Duration timeout, Runnable onTimeout) {
        this.uiThread = Objects.requireNonNull(uiThread, "uiThread must not be null");
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
        this.onTimeout = Objects.requireNonNull(onTimeout, "onTimeout must not be null");
    }

    /**
     * @param quietErrors the caller handles an ERROR reply itself, so the usual error toast is skipped
     */
    public CompletableFuture<NetworkMessage> register(String requestId, boolean quietErrors) {
        CompletableFuture<NetworkMessage> reply = new CompletableFuture<>();
        ScheduledFuture<?> expiry = timer.schedule(
                () -> uiThread.execute(() -> expire(requestId)),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
        );
        entries.put(requestId, new Entry(reply, quietErrors, expiry));
        return reply;
    }

    public boolean isQuiet(String requestId) {
        Entry entry = requestId == null ? null : entries.get(requestId);
        return entry != null && entry.quietErrors();
    }

    /** Completes the request this message answers; call after the message has been applied to the state. */
    public void complete(NetworkMessage message) {
        Entry entry = message.requestId() == null ? null : entries.remove(message.requestId());
        if (entry != null) {
            entry.timeout().cancel(false);
            entry.reply().complete(message);
        }
    }

    public void fail(String requestId, Throwable cause) {
        Entry entry = entries.remove(requestId);
        if (entry != null) {
            entry.timeout().cancel(false);
            entry.reply().completeExceptionally(cause);
        }
    }

    /** Fails everything still waiting, e.g. when the connection drops. */
    public void failAll(Throwable cause) {
        List.copyOf(entries.keySet()).forEach(requestId -> fail(requestId, cause));
    }

    public int size() {
        return entries.size();
    }

    private void expire(String requestId) {
        Entry entry = entries.remove(requestId);
        if (entry != null) {
            entry.reply().completeExceptionally(new TimeoutException("No reply to request " + requestId));
            onTimeout.run();
        }
    }

    @Override
    public void close() {
        timer.shutdownNow();
    }
}
