package com.bangzachery.vitae.vitaemanager.servercontrol;

import org.bukkit.configuration.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServerControlStoreTest {
    @TempDir
    Path directory;

    @Test
    void importsLegacyMaintenanceWithoutRewritingLegacyFile()
            throws Exception {
        Path legacy = directory.resolve("config.yml");
        String old =
                "maintenance_mode: true\ncustom: jangan-hapus\n";

        Files.writeString(legacy, old);

        ServerControlStore store = store();

        assertTrue(store.initialize(legacy).maintenance());
        assertEquals(old, Files.readString(legacy));
        assertTrue(store.load().maintenance());
    }

    @Test
    void emptyLegacyConfigDefaultsToOpenServer()
            throws Exception {
        Path legacy = directory.resolve("config.yml");
        Files.writeString(legacy, "");

        ServerControlState state = store().initialize(legacy);

        assertFalse(state.maintenance());
        assertFalse(state.chatMuted());
        assertTrue(state.worlds().isEmpty());
        assertEquals("", Files.readString(legacy));
    }

    @Test
    void rejectsBadLegacyTypeBeforeCreatingStateFile()
            throws Exception {
        Path legacy = directory.resolve("config.yml");

        Files.writeString(
                legacy, "maintenance_mode: 'true'\n");

        assertThrows(
                IllegalArgumentException.class,
                () -> store().initialize(legacy));

        assertFalse(Files.exists(file()));
    }

    @Test
    void existingStateWinsOverStaleLegacyMaintenance()
            throws Exception {
        ServerControlStore store = store();

        ServerControlState state =
                new ServerControlState(false, true, Map.of());

        store.save(state);

        Path legacy = directory.resolve("config.yml");

        Files.writeString(
                legacy, "maintenance_mode: true\n");

        assertEquals(state, store.initialize(legacy));
    }

    @Test
    void invalidExistingStateIsNotReplacedWithDefaults()
            throws Exception {
        String broken = "worlds: [broken\n";

        Files.writeString(file(), broken);

        assertThrows(
                InvalidConfigurationException.class,
                () -> store().initialize(
                        directory.resolve("missing-config.yml")));

        assertEquals(broken, Files.readString(file()));
    }

    @Test
    void savesGlobalAndWorldRulesAcrossNewStoreInstance()
            throws Exception {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        var state = new ServerControlState(
                true, true,
                Map.of(
                        first,
                        new ServerControlState.WorldRules(
                                false, true),
                        second,
                        new ServerControlState.WorldRules(
                                true, false)));

        store().save(state);

        assertEquals(state, store().load());
    }

    @Test
    void rejectsInvalidSchemaTypesAndWorldIdsWithoutChangingFile()
            throws Exception {
        String base =
                "state-version: 1\n"
                        + "maintenance: false\n"
                        + "chat-muted: false\n";

        for (String bad : new String[]{
                "state-version: 2\n"
                        + "maintenance: false\n"
                        + "chat-muted: false\n",
                "state-version: 1\n"
                        + "maintenance: 'false'\n"
                        + "chat-muted: false\n",
                "state-version: 1\nmaintenance: false\n",
                base + "worlds: wrong\n",
                base + "worlds:\n"
                        + "  invalid-id:\n"
                        + "    pvp: true\n"
                        + "    mob-spawning: true\n",
                base + "worlds:\n"
                        + "  " + UUID.randomUUID() + ":\n"
                        + "    pvp: true\n"}) {
            Files.writeString(file(), bad);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> store().load());

            assertEquals(bad, Files.readString(file()));
        }
    }

    @Test
    void failedAtomicReplacementCleansTemporaryFileAndPreservesDestination()
            throws Exception {
        Files.createDirectory(file());

        Path marker = file().resolve("existing-data");
        Files.writeString(marker, "preserve");

        assertThrows(
                IOException.class,
                () -> store().save(
                        new ServerControlState(
                                false, false, Map.of())));

        assertEquals("preserve", Files.readString(marker));

        try (var paths = Files.list(directory)) {
            assertTrue(paths.noneMatch(
                    path -> path.getFileName().toString()
                            .endsWith(".tmp")));
        }
    }

    @Test
    void immutableSnapshotDoesNotFollowCallerMapMutations() {
        UUID id = UUID.randomUUID();

        var input =
                new HashMap<UUID, ServerControlState.WorldRules>();

        var original =
                new ServerControlState(false, false, input);

        input.put(
                id, new ServerControlState.WorldRules(true, true));

        assertTrue(original.worlds().isEmpty());

        assertThrows(
                UnsupportedOperationException.class,
                () -> original.worlds().put(
                        id,
                        new ServerControlState.WorldRules(
                                false, false)));

        var changed = original
                .withWorld(
                        id,
                        new ServerControlState.WorldRules(
                                false, true))
                .withMaintenance(true)
                .withChatMuted(true);

        assertTrue(changed.maintenance());
        assertTrue(changed.chatMuted());
        assertEquals(1, changed.worlds().size());
        assertTrue(original.worlds().isEmpty());
    }

    private Path file() {
        return directory.resolve("server-control.yml");
    }

    private ServerControlStore store() {
        return new ServerControlStore(file());
    }
}