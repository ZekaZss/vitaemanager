package com.bangzachery.vitae.vitaemanager.whisper;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WhisperAreaTest {
    private final UUID world = UUID.randomUUID();
    @Test void reversedCornersIncludeWholeBlocksAndRespectWorldAndUpperFace() {
        var area = WhisperArea.between(world, 5, 70, 8, -2, 64, 3);
        assertTrue(area.contains(world, -2, 64, 3));
        assertTrue(area.contains(world, 5.99, 70.99, 8.99));
        assertFalse(area.contains(world, 6, 70, 8));
        assertFalse(area.contains(world, -2.01, 64, 3));
        assertFalse(area.contains(UUID.randomUUID(), 0, 65, 4));
        assertFalse(area.contains(world, Double.NaN, 65, 4));
    }
    @Test void equalCornersMakeAOneBlockAreaAndSweptMovementCannotSkipIt() {
        var area = WhisperArea.between(world, 0, 64, 0, 0, 64, 0);
        assertTrue(area.crossed(world, -3, 64.5, 0.5, 3, 64.5, 0.5));
        assertTrue(area.crossed(world, -3, 64.5, 0.5, 0.5, 64.5, 0.5));
        assertFalse(area.crossed(world, 0.5, 64.5, 0.5, 3, 64.5, 0.5));
        assertFalse(area.crossed(world, -3, 65, 0.5, 3, 65, 0.5));
        assertFalse(area.crossed(world, -3, 66, 0.5, 3, 66, 0.5));
        assertFalse(area.crossed(UUID.randomUUID(), -3, 64.5, 0.5, 3, 64.5, 0.5));
    }
    @Test void negativeCoordinatesAndVerticalEntryAreSupported() {
        var area = WhisperArea.between(world, -2, -10, -3, -2, -10, -3);
        assertTrue(area.contains(world, -1.5, -9.5, -2.5));
        assertTrue(area.crossed(world, -1.5, -12, -2.5, -1.5, -8, -2.5));
        assertFalse(area.crossed(world, Double.POSITIVE_INFINITY, 0, 0, 0, 0, 0));
    }
    @Test void oversizedAndOverflowingSelectionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> WhisperArea.between(world, 0, 0, 0, 256, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> WhisperArea.between(world, Integer.MIN_VALUE, 0, 0, Integer.MAX_VALUE, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new WhisperArea(world, 2, 0, 0, 1, 0, 0));
    }
}