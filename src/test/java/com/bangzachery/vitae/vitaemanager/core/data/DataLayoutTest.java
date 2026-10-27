package com.bangzachery.vitae.vitaemanager.core.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class DataLayoutTest {
    @TempDir Path root;
    @Test void copiesExactBytesBacksUpAndKeepsLegacy() throws Exception {
        Files.writeString(root.resolve("npc.json"), "custom data\n");
        DataLayout.migrate(root, "npc.json", "npc/npc.json");
        assertEquals("custom data\n", Files.readString(root.resolve("npc/npc.json")));
        assertTrue(Files.exists(root.resolve("npc.json")));
        assertEquals("custom data\n", Files.readString(root.resolve("migration/layout-backup/npc.json")));
        Files.writeString(root.resolve("npc/npc.json"), "new authoritative data");
        DataLayout.migrate(root, "npc.json", "npc/npc.json");
        assertEquals("new authoritative data", Files.readString(root.resolve("npc/npc.json")));
    }
    @Test void conflictingUntrackedCopiesAreNeverChosenSilently() throws Exception {
        Files.writeString(root.resolve("npc.json"), "old");
        Files.createDirectories(root.resolve("npc"));
        Files.writeString(root.resolve("npc/npc.json"), "other");
        assertThrows(IOException.class, () -> DataLayout.migrate(root, "npc.json", "npc/npc.json"));
        assertEquals("other", Files.readString(root.resolve("npc/npc.json")));
    }
    @Test void resumesInterruptedCopyAndRejectsEditedLegacy() throws Exception {
        Files.writeString(root.resolve("npc.json"), "original");
        DataLayout.migrate(root, "npc.json", "npc/npc.json");
        Files.delete(root.resolve("migration/layout-journal/npc.json.complete"));
        Files.delete(root.resolve("npc/npc.json")); // interrupted/lost destination; original hash remains
        DataLayout.migrate(root, "npc.json", "npc/npc.json");
        assertEquals("original", Files.readString(root.resolve("npc/npc.json")));
        Files.writeString(root.resolve("npc.json"), "external edit");
        assertThrows(IOException.class, () -> DataLayout.migrate(root, "npc.json", "npc/npc.json"));
    }
    @Test void doesNotCopyDefaultsOrTouchMoney() throws Exception {
        Files.writeString(root.resolve("viti.yml"), "highest_balance_exact: '123.456'");
        DataLayout.initialize(root);
        assertEquals("highest_balance_exact: '123.456'", Files.readString(root.resolve("viti.yml")));
        assertFalse(Files.exists(root.resolve("npc/npc.json")));
    }
    @Test void preparedButUnverifiedTargetMustMatchSource() throws Exception {
        Files.writeString(root.resolve("npc.json"), "original");
        DataLayout.migrate(root, "npc.json", "npc/npc.json");
        Files.delete(root.resolve("migration/layout-journal/npc.json.complete"));
        Files.writeString(root.resolve("npc/npc.json"), "unexpected concurrent target");
        assertThrows(IOException.class, () -> DataLayout.migrate(root, "npc.json", "npc/npc.json"));
    }
    @Test void missingAuthoritativeDataNeverResurrectsStaleLegacyBackup() throws Exception {
        Files.writeString(root.resolve("npc.json"), "original");
        DataLayout.migrate(root, "npc.json", "npc/npc.json");
        Files.writeString(root.resolve("npc/npc.json"), "new progress");
        Files.delete(root.resolve("npc/npc.json"));
        assertThrows(IOException.class, () -> DataLayout.migrate(root, "npc.json", "npc/npc.json"));
        assertFalse(Files.exists(root.resolve("npc/npc.json")));
    }
}
