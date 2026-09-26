package com.bomberman.clientfx.ui;

import com.bomberman.clientfx.ui.screen.PlaceholderScreen;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** {@link Navigator} backed by the {@link AppShell}; screens not built yet show a placeholder. */
public final class ScreenNavigator implements Navigator {

    private final AppShell shell;
    private final Map<ScreenId, Screen> screens = new EnumMap<>(ScreenId.class);
    private ScreenId current;

    public ScreenNavigator(AppShell shell) {
        this.shell = Objects.requireNonNull(shell, "shell must not be null");
    }

    public void register(ScreenId id, Screen screen) {
        screens.put(id, Objects.requireNonNull(screen, "screen must not be null"));
    }

    @Override
    public void show(ScreenId screen) {
        if (screen == current) {
            return;
        }
        current = screen;
        shell.show(screens.computeIfAbsent(screen, PlaceholderScreen::new));
    }

    @Override
    public ScreenId current() {
        return current;
    }
}
