package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;
import static org.junit.jupiter.api.Assertions.*;

class EvolutionAftermathDataTest {
    @TempDir Path directory;
    EvolutionStore store() { return new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds()); }
    @Test void oldAltarsDepositsAndTicketsMigrateWithoutChangingMusicOrItems() throws Exception {
        var altar = EvolutionStoreTest.altar(); UUID owner = EvolutionStoreTest.OWNER;
        var deposit = new Deposit(UUID.randomUUID(), owner, "api", altar.table(), Map.of(0, "AQ=="), null);
        State state = EvolutionStoreTest.initial().deposit(deposit);
        var yaml = store().document(state); yaml.set("altars.api.aftermath", null); Path file = directory.resolve("evolution.yml"); yaml.save(file.toFile());
        byte[] before = Files.readAllBytes(file); State migrated = store().initialize(); assertEquals(state, migrated);
        assertEquals(100, migrated.altars().get("api").aftermath().ticks()); assertEquals(altar.music(), migrated.altars().get("api").music());
        assertEquals(state.deposits(), migrated.deposits());
        List<Path> backups; try (var files = Files.list(directory)) { backups = files.filter(p -> p.toString().endsWith(".bak")).toList(); }
        assertEquals(1, backups.size()); assertArrayEquals(before, Files.readAllBytes(backups.getFirst()));
        assertEquals(migrated, store().initialize()); try (var files = Files.list(directory)) { assertEquals(1, files.filter(p -> p.toString().endsWith(".bak")).count()); }
    }
    @Test void revealCheckpointSurvivesCrashAndCannotRefundOldWeapon() throws Exception {
        var altar = EvolutionStoreTest.altar(); var owner = EvolutionStoreTest.OWNER;
        Ticket ticket = new Ticket(UUID.randomUUID(), owner, "api", altar.table(), Phase.PREPARED, List.of("AQ=="), "Ag==");
        State state = EvolutionStoreTest.initial().prepare(ticket).phase(owner, ticket.id(), Phase.ACTIVE).phase(owner, ticket.id(), Phase.COMMITTING).phase(owner, ticket.id(), Phase.REVEALING);
        store().save(state); assertEquals(state, store().load());
        assertThrows(IllegalArgumentException.class, () -> state.phase(owner, ticket.id(), Phase.REFUNDING));
        assertThrows(IllegalArgumentException.class, () -> state.phase(owner, ticket.id(), Phase.RETURNED));
        assertEquals(Phase.DELIVERED, state.phase(owner, ticket.id(), Phase.DELIVERED).tickets().get(owner).phase());
        assertEquals(Phase.DISCARDED, state.phase(owner, ticket.id(), Phase.DISCARDED).tickets().get(owner).phase());
    }
    @Test void endingSettingsDoNotChangeExplosionOrMusicTimeline() {
        var yaml = store().document(EvolutionStoreTest.initial()); yaml.set("altars.api.aftermath.duration", 12);
        var altar = store().parse(yaml).altars().get("api"); assertEquals(240, altar.aftermath().ticks());
        assertEquals(180, altar.ticks()); assertEquals(180, altar.music().ticks());
        yaml.set("altars.api.aftermath.enabled", false); assertFalse(store().parse(yaml).altars().get("api").aftermath().enabled());
    }
    @Test void invalidEndingCannotReplaceSavedState() throws Exception {
        store().save(EvolutionStoreTest.initial()); byte[] before = Files.readAllBytes(directory.resolve("evolution.yml"));
        for (double invalid : List.of(0.0, 1.0, 16.0, Double.NaN)) {
            var yaml = store().document(EvolutionStoreTest.initial()); yaml.set("altars.api.aftermath.duration", invalid);
            assertThrows(IllegalArgumentException.class, () -> store().parse(yaml));
        }
        assertArrayEquals(before, Files.readAllBytes(directory.resolve("evolution.yml")));
    }
}
