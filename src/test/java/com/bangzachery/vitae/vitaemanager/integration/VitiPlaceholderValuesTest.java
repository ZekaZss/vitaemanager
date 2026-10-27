package com.bangzachery.vitae.vitaemanager.integration;

import com.bangzachery.vitae.vitaemanager.economy.VitiLedger;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class VitiPlaceholderValuesTest {
    @Test void resolvesOnlyRequestedPlayerFromOfficialSnapshotWithoutChangingLedger() {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        VitiLedger ledger = new VitiLedger(Map.of(
                first, new VitiLedger.Account("first", new BigDecimal("12500.75"), new BigDecimal("50000.125")),
                second, new VitiLedger.Account("second", new BigDecimal("2.65625"), new BigDecimal("2.65625"))), Map.of(), Set.of());
        assertEquals("12.500,75", VitiPlaceholderValues.resolve(ledger, first, "viti"));
        assertEquals("12500.75", VitiPlaceholderValues.resolve(ledger, first, "viti_raw"));
        assertEquals("12,5k", VitiPlaceholderValues.resolve(ledger, first, "viti_short"));
        assertEquals("50.000,125", VitiPlaceholderValues.resolve(ledger, first, "viti_highest"));
        assertEquals("2,65625", VitiPlaceholderValues.resolve(ledger, second, "viti"));
        assertEquals("0", VitiPlaceholderValues.resolve(ledger, UUID.randomUUID(), "viti"));
        assertEquals("0", VitiPlaceholderValues.resolve(ledger, null, "viti_raw"));
        assertNull(VitiPlaceholderValues.resolve(ledger, first, "quest"));
        assertEquals(new BigDecimal("12500.75"), ledger.accounts().get(first).balance());
        assertTrue(ledger.redeemed().isEmpty());
    }
}
