package com.bomberman.server.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** One single-threaded authoritative loop for one room. */
public final class RoomGameLoop implements AutoCloseable {

    public static final int TICKS_PER_SECOND = 20;
    public static final int MAX_QUEUED_COMMANDS = 512;

    private static final Logger log = LoggerFactory.getLogger(RoomGameLoop.class);
    private static final long TICK_PERIOD_MILLIS = 1_000 / TICKS_PER_SECOND;
    /** The map is resent at least this often even when unchanged, as a safety keyframe. */
    private static final int MAP_KEYFRAME_TICKS = TICKS_PER_SECOND;

    private final String roomId;
    private final BombermanGame game;
    private final Clock clock;
    private final GameLoopListener listener;
    private final BlockingQueue<GameCommand> commandQueue =
            new ArrayBlockingQueue<>(MAX_QUEUED_COMMANDS);
    private final ScheduledExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean();

    private long tick;
    private int sentMapVersion = -1;

    public RoomGameLoop(
            String roomId,
            BombermanGame game,
            Clock clock,
            GameLoopListener listener
    ) {
        this.roomId = Objects.requireNonNull(roomId, "roomId must not be null");
        this.game = Objects.requireNonNull(game, "game must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.listener = Objects.requireNonNull(listener, "listener must not be null");
        this.executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("game-loop-" + roomId).factory()
        );
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            executor.scheduleAtFixedRate(
                    this::runTickSafely,
                    0,
                    TICK_PERIOD_MILLIS,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    public boolean enqueue(GameCommand command) {
        return running.get() && commandQueue.offer(Objects.requireNonNull(command));
    }

    public boolean isRunning() {
        return running.get();
    }

    public BombermanGame getGame() {
        return game;
    }

    @Override
    public void close() {
        running.set(false);
        commandQueue.clear();
        executor.shutdownNow();
    }

    private void runTickSafely() {
        if (!running.get()) {
            return;
        }
        try {
            Instant tickTime = clock.instant();
            drainCommands(tickTime);
            GameTickResult result = game.tick(tickTime);
            tick++;
            // Every tick (20 Hz) so movement reaches clients evenly; the map, most of the
            // payload, only when a crate broke or as a periodic keyframe.
            int mapVersion = game.mapVersion();
            boolean includeMap = mapVersion != sentMapVersion
                    || tick % MAP_KEYFRAME_TICKS == 0
                    || result.outcome() != null;
            if (includeMap) {
                sentMapVersion = mapVersion;
            }
            listener.onGameState(roomId, game.createSnapshot(tick, tickTime, includeMap));
            if (result.outcome() != null && running.compareAndSet(true, false)) {
                listener.onGameOver(roomId, game, result.outcome());
                executor.shutdown();
            }
        } catch (RuntimeException exception) {
            log.error("Game loop failed for room {}", roomId, exception);
            close();
        }
    }

    private void drainCommands(Instant tickTime) {
        GameCommand command;
        while ((command = commandQueue.poll()) != null) {
            command.execute(game, tickTime);
        }
        game.processQueuedInputs(tickTime);
    }
}
