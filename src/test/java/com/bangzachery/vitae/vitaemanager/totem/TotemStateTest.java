package com.bangzachery.vitae.vitaemanager.totem;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TotemStateTest {
    private static final long FIFTEEN_HOURS = 54_000_000L;

    @Test void absorptionAddsExactlyOneChargeAndDoesNotStartCooldown() {
        var empty = new TotemState(0, 0);
        assertEquals(new TotemState(1, 0), empty.absorb(3, 1000));
        assertEquals(0, empty.count());
    }

    @Test void fullLimitOrActiveCooldownDoesNotMutateOriginal() {
        var full = new TotemState(3, 0);
        assertEquals("totem-full", assertThrows(TotemState.Failure.class,
                () -> full.absorb(3, 1000)).key());
        var cooling = new TotemState(1, 1001);
        assertEquals("totem-cooldown", assertThrows(TotemState.Failure.class,
                () -> cooling.absorb(3, 1000)).key());
        assertEquals(new TotemState(1, 1001), cooling);
    }

    @Test void cooldownExpiresAtExactMillisecondBoundary() {
        var state = new TotemState(1, 2000);
        assertThrows(TotemState.Failure.class, () -> state.absorb(3, 1999));
        assertEquals(2, state.absorb(3, 2000).count());
        assertEquals(2, state.absorb(3, 2001).count());
        assertEquals(0, state.remainingSeconds(2000));
    }

    @Test void firstUseConsumesOneAndStartsFifteenHourCooldown() {
        var original = new TotemState(3, 0);
        var used = original.use(1000, FIFTEEN_HOURS);
        assertEquals(new TotemState(2, 54_001_000), used);
        assertEquals(3, original.count());
    }

    @Test void storedChargesRemainUsableDuringCooldownWithoutExtendingIt() {
        var first = new TotemState(3, 0).use(1000, FIFTEEN_HOURS);
        var second = first.use(2000, FIFTEEN_HOURS);
        var third = second.use(3000, FIFTEEN_HOURS);
        assertEquals(0, third.count());
        assertEquals(first.cooldownUntil(), second.cooldownUntil());
        assertEquals(first.cooldownUntil(), third.cooldownUntil());
        assertThrows(TotemState.Failure.class, () -> third.use(4000, FIFTEEN_HOURS));
    }

    @Test void expiredCooldownStartsNewWindowAndZeroDurationDisablesWaiting() {
        assertEquals(new TotemState(1, 54_002_000), new TotemState(2, 2000).use(2000, FIFTEEN_HOURS));
        var noWait = new TotemState(1, 0).use(1000, 0);
        assertEquals(1, noWait.absorb(3, 1000).count());
    }

    @Test void loweringLimitPreservesExistingExcessCharges() {
        var old = new TotemState(10, 0);
        assertThrows(TotemState.Failure.class, () -> old.absorb(3, 1000));
        assertEquals(9, old.use(1000, FIFTEEN_HOURS).count());
        assertEquals(10, old.count());
    }

    @Test void cooldownResetKeepsCountAndEnablesRefill() {
        var old = new TotemState(2, 54_001_000);
        var reset = old.resetCooldown();
        assertEquals(new TotemState(2, 0), reset);
        assertEquals(3, reset.absorb(3, 1000).count());
        assertEquals(54_001_000, old.cooldownUntil());
    }

    @Test void remainingTimeRoundsUpWithoutLongOverflow() {
        assertEquals(1, new TotemState(0, 1001).remainingSeconds(1000));
        assertEquals(2, new TotemState(0, 2001).remainingSeconds(1000));
        assertEquals(1, new TotemState(0, 2000).remainingSeconds(1000));
        assertEquals(9_223_372_036_854_776L, new TotemState(0, Long.MAX_VALUE).remainingSeconds(0));
    }

    @Test void invalidDataLimitOrClockRefusesTransition() {
        assertThrows(IllegalArgumentException.class, () -> new TotemState(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new TotemState(0, -1));
        var state = new TotemState(1, 0);
        for (int limit : new int[]{-1,0,1001}) {
            assertThrows(IllegalArgumentException.class, () -> state.absorb(limit, 1000));
        }
        assertThrows(IllegalArgumentException.class, () -> state.absorb(3, -1));
        assertThrows(IllegalArgumentException.class, () -> state.use(1000, -1));
        assertThrows(ArithmeticException.class, () -> state.use(Long.MAX_VALUE, 1));
        assertEquals(new TotemState(1, 0), state);
    }

    @Test void fatalChecksActualFinitePostReductionDamageIncludingEquality() {
        assertFalse(TotemState.fatal(20, 19.999));
        assertTrue(TotemState.fatal(20, 20));
        assertTrue(TotemState.fatal(20, 50));
        assertFalse(TotemState.fatal(0, 50));
        assertFalse(TotemState.fatal(20, -1));
        assertFalse(TotemState.fatal(20, Double.NaN));
        assertFalse(TotemState.fatal(20, Double.POSITIVE_INFINITY));
    }
}