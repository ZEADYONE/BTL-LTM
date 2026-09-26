package com.bomberman.clientfx.game;

import com.bomberman.common.enums.Direction;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InputControllerTest {

    @Test
    void sendsImmediatelyThenRepeatsEvery140Millis() {
        List<Direction> moves = new ArrayList<>();
        InputController input = new InputController(moves::add, () -> { }, () -> true);

        input.pressDirection(Direction.RIGHT, ms(0));
        input.update(ms(139));
        input.update(ms(140));
        input.update(ms(280));
        input.update(ms(420));

        assertEquals(List.of(Direction.RIGHT, Direction.RIGHT, Direction.RIGHT, Direction.RIGHT), moves);
    }

    @Test
    void newestDirectionWinsAndReleaseReturnsToTheStillHeldDirection() {
        List<Direction> moves = new ArrayList<>();
        InputController input = new InputController(moves::add, () -> { }, () -> true);

        input.pressDirection(Direction.RIGHT, ms(0));
        input.pressDirection(Direction.UP, ms(10));
        input.releaseDirection(Direction.UP, ms(20));

        assertEquals(List.of(Direction.RIGHT, Direction.UP, Direction.RIGHT), moves);
    }

    @Test
    void spaceDoesNotRepeatUntilReleased() {
        AtomicInteger bombs = new AtomicInteger();
        InputController input = new InputController(direction -> { }, bombs::incrementAndGet, () -> true);

        input.pressBomb();
        input.pressBomb();
        input.releaseBomb();
        input.pressBomb();

        assertEquals(2, bombs.get());
    }

    @Test
    void blockedInputNeverSendsAndDoesNotBurstOnResume() {
        List<Direction> moves = new ArrayList<>();
        AtomicBoolean allowed = new AtomicBoolean(false);
        InputController input = new InputController(moves::add, () -> { }, allowed::get);

        input.pressDirection(Direction.DOWN, ms(0));
        input.update(ms(500));
        allowed.set(true);
        input.update(ms(501));
        input.update(ms(640));

        assertEquals(List.of(Direction.DOWN), moves);
    }

    @Test
    void resetClearsHeldKeys() {
        List<Direction> moves = new ArrayList<>();
        InputController input = new InputController(moves::add, () -> { }, () -> true);
        input.pressDirection(Direction.LEFT, ms(0));

        input.reset();
        input.update(ms(500));

        assertEquals(List.of(Direction.LEFT), moves);
    }

    private static long ms(long millis) {
        return millis * 1_000_000L;
    }
}
