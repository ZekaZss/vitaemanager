package com.bangzachery.vitae.vitaemanager.profile;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StackExchangeTest {
    private StackExchange.Result move(int slot, int cursor, boolean similar, boolean right) {
        return StackExchange.move(slot, cursor, similar, 64, 64, 99, right);
    }

    @Test void leftPickupRemovesExactlyWhatCursorReceives() {
        assertEquals(new StackExchange.Result(0, 37, false), move(37, 0, false, false));
    }

    @Test void rightPickupRoundsOddStackUp() {
        assertEquals(new StackExchange.Result(18, 19, false), move(37, 0, false, true));
        assertEquals(new StackExchange.Result(0, 1, false), move(1, 0, false, true));
    }

    @Test void depositRespectsItemLimitAndRetainsOverflowOnCursor() {
        assertEquals(new StackExchange.Result(64, 15, false), move(60, 19, true, false));
        assertEquals(new StackExchange.Result(61, 18, false), move(60, 19, true, true));
    }

    @Test void depositingIntoEmptySlotUsesCursorItemLimit() {
        assertEquals(new StackExchange.Result(16, 0, false),
                StackExchange.move(0, 16, false, 99, 16, 99, false));
        assertEquals(new StackExchange.Result(1, 15, false),
                StackExchange.move(0, 16, false, 99, 16, 99, true));
    }

    @Test void incompatibleItemsSwapEvenWithDifferentMaximums() {
        assertEquals(new StackExchange.Result(1, 64, true),
                StackExchange.move(64, 1, false, 64, 1, 99, false));
        assertEquals(new StackExchange.Result(64, 1, true),
                StackExchange.move(1, 64, false, 1, 64, 99, true));
    }

    @Test void armorCapacityPreventsOversizedPlacementAndSwap() {
        assertEquals(new StackExchange.Result(1, 15, false),
                StackExchange.move(0, 16, false, 99, 64, 1, false));
        assertEquals(new StackExchange.Result(1, 16, false),
                StackExchange.move(1, 16, false, 1, 64, 1, false));
    }

    @Test void fullStacksAndAbnormalLegacyStacksArePreserved() {
        assertEquals(new StackExchange.Result(64, 32, false), move(64, 32, true, false));
        assertEquals(new StackExchange.Result(80, 0, false), move(80, 0, false, false));
        assertEquals(new StackExchange.Result(0, 80, false), move(0, 80, false, true));
    }

    @Test void emptyClickDoesNothing() {
        assertEquals(new StackExchange.Result(0, 0, false), move(0, 0, false, false));
        assertEquals(new StackExchange.Result(0, 0, false), move(0, 0, false, true));
    }

    @Test void countsAndItemIdentitiesAreConservedAcrossAllNormalAmounts() {
        for (int limit : new int[]{1, 16, 64, 99}) {
            for (int slot = 0; slot <= limit; slot++) {
                for (int cursor = 0; cursor <= limit; cursor++) {
                    for (boolean right : new boolean[]{false, true}) {
                        for (boolean similar : new boolean[]{false, true}) {
                            var result = StackExchange.move(slot, cursor, similar, limit, limit, 99, right);
                            assertEquals(slot + cursor, result.slot() + result.cursor());
                            assertTrue(result.slot() >= 0 && result.slot() <= limit);
                            assertTrue(result.cursor() >= 0 && result.cursor() <= limit);
                            if (!similar && slot > 0 && cursor > 0) {
                                assertTrue(result.swapped());
                                assertEquals(cursor, result.slot());
                                assertEquals(slot, result.cursor());
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> move(-1, 0, false, false));
        assertThrows(IllegalArgumentException.class, () -> move(0, -1, false, false));
        assertThrows(IllegalArgumentException.class, () -> StackExchange.move(0, 0, false, 0, 64, 99, false));
        assertThrows(IllegalArgumentException.class, () -> StackExchange.move(0, 0, false, 64, 0, 99, false));
        assertThrows(IllegalArgumentException.class, () -> StackExchange.move(0, 0, false, 64, 64, 0, false));
    }
}