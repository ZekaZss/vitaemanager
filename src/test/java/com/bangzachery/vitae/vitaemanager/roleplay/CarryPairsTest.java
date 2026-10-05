package com.bangzachery.vitae.vitaemanager.roleplay;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class CarryPairsTest {
    @Test void preventsSelfCarryReversalChainsAndSharedRiders() {
        var pairs = new CarryPairs();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> pairs.add(a, a));
        pairs.add(a, b);
        for (UUID[] pair : new UUID[][]{{b,a},{b,c},{c,a},{c,b},{a,c}}) {
            assertThrows(IllegalArgumentException.class, () -> pairs.add(pair[0], pair[1]));
        }
        assertEquals(b, pairs.rider(a));
        assertEquals(1, pairs.snapshot().size());
        assertFalse(pairs.available(a));
        assertFalse(pairs.available(b));
        assertTrue(pairs.available(c));
    }

    @Test void staleReleaseCannotDetachDifferentPairAndSnapshotCannotMutateOwnership() {
        var pairs = new CarryPairs();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        pairs.add(a, b);
        var snapshot = pairs.snapshot();
        pairs.remove(a, c);
        assertEquals(b, pairs.rider(a));
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        pairs.remove(a, b);
        pairs.add(a, c);
        pairs.remove(a, b);
        assertEquals(c, pairs.rider(a));
        assertEquals(b, snapshot.get(a));
        assertTrue(pairs.available(b));
    }

    @Test void releasingOnePairLeavesIndependentCarriersUntouched() {
        var pairs = new CarryPairs();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), d = UUID.randomUUID();
        pairs.add(a, b);
        pairs.add(c, d);
        pairs.remove(a, b);
        assertTrue(pairs.available(a));
        assertTrue(pairs.available(b));
        assertEquals(d, pairs.rider(c));
    }
}