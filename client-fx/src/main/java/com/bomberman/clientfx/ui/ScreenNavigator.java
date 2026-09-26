package com.bomberman.clientfx.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** {@link Navigator} backed by the {@link AppShell}; unregistered screens come from a fallback factory. */
public final class ScreenNavigator implements Navigator {

    private final AppShell shell;
    private final Map<ScreenId, Screen> screens = new EnumMap<>(ScreenId.class);
    private Function<ScreenId, Screen> fallback;
    private ScreenId current;

    public ScreenNavigator(AppShell shell) {
        this.shell = Objects.requireNonNull(shell, "shell must not be null");
    }

    public void register(ScreenId id, Screen screen) {
        screens.put(id, Objects.requireNonNull(screen, "screen must not be null"));
    }

    /** Builds screens that are not registered yet (placeholders until their phase). */
    public void setFallback(Function<ScreenId, Screen> fallback) {
        this.fallback = fallback;
    }

    @Override
    public void show(ScreenId screen) {
        if (screen == current) {
            return;
        }
        Screen target = screens.get(screen);
        if (target == null) {
            target = Objects.requireNonNull(fallback, "No screen registered for " + screen).apply(screen);
            screens.put(screen, target);
        }
        current = screen;
        shell.show(target);
    }

    @Override
    public ScreenId current() {
        return current;
    }
}
