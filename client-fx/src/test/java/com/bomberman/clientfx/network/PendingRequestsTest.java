package com.bomberman.clientfx.network;

import com.bomberman.common.enums.MessageType;
import com.bomberman.common.message.NetworkMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingRequestsTest {

    private final AtomicInteger timeouts = new AtomicInteger();
    private final PendingRequests pending = new PendingRequests(Runnable::run, Duration.ofMillis(150), timeouts::incrementAndGet);

    @AfterEach
    void close() {
        pending.close();
    }

    @Test
    void replyWithTheSameRequestIdCompletesTheRequest() {
        CompletableFuture<NetworkMessage> reply = pending.register("a", false);

        pending.complete(NetworkMessage.withoutPayload(MessageType.PONG, "other"));
        assertFalse(reply.isDone());

        pending.complete(NetworkMessage.withoutPayload(MessageType.LOGIN_RESPONSE, "a"));
        assertEquals(MessageType.LOGIN_RESPONSE, reply.join().type());
        assertEquals(0, pending.size());
    }

    @Test
    void requestWithoutReplyExpiresOnce() throws Exception {
        CompletableFuture<NetworkMessage> reply = pending.register("a", false);

        ExecutionException failure = assertThrows(ExecutionException.class, () -> reply.get(2, TimeUnit.SECONDS));
        assertInstanceOf(TimeoutException.class, failure.getCause());
        assertEquals(1, timeouts.get());

        pending.complete(NetworkMessage.withoutPayload(MessageType.LOGIN_RESPONSE, "a"));
        Thread.sleep(200);
        assertEquals(1, timeouts.get());
    }

    @Test
    void lateReplyAfterAnsweredRequestIsIgnored() throws InterruptedException {
        CompletableFuture<NetworkMessage> reply = pending.register("a", false);
        pending.complete(NetworkMessage.withoutPayload(MessageType.LOGIN_RESPONSE, "a"));
        pending.complete(NetworkMessage.withoutPayload(MessageType.ERROR, "a"));

        Thread.sleep(250);
        assertEquals(MessageType.LOGIN_RESPONSE, reply.join().type());
        assertEquals(0, timeouts.get(), "answered requests must not time out");
    }

    @Test
    void quietFlagIsVisibleOnlyWhileWaiting() {
        pending.register("quiet", true);
        pending.register("loud", false);

        assertTrue(pending.isQuiet("quiet"));
        assertFalse(pending.isQuiet("loud"));
        assertFalse(pending.isQuiet(null));

        pending.complete(NetworkMessage.withoutPayload(MessageType.ERROR, "quiet"));
        assertFalse(pending.isQuiet("quiet"));
    }

    @Test
    void failAllCompletesEverythingExceptionally() {
        CompletableFuture<NetworkMessage> first = pending.register("a", false);
        CompletableFuture<NetworkMessage> second = pending.register("b", true);

        pending.failAll(new IOException("Connection lost"));

        assertTrue(first.isCompletedExceptionally());
        assertTrue(second.isCompletedExceptionally());
        assertEquals(0, pending.size());
    }
}
