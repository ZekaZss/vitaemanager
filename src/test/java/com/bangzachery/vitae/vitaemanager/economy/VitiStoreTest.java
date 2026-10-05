package com.bangzachery.vitae.vitaemanager.economy;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VitiStoreTest {
    @TempDir
    Path directory;

    private final UUID id = UUID.randomUUID();

    @Test
    void createsEmptyFileAndDoesNotCopyBundledSample()
            throws Exception {
        VitiLedger ledger = store().initialize();

        assertTrue(ledger.accounts().isEmpty());
        assertTrue(ledger.notes().isEmpty());
        assertEquals("players: {}\n", Files.readString(file()));
    }

    @Test
    void importsLegacyDoubleAndKeepsFractionalPrecisionAndPeak()
            throws Exception {
        String original = "players:\n  " + id + ":\n"
                + "    name: One\n"
                + "    balance: 0.123456789\n"
                + "    highest_balance: 50000.0\n";

        Files.writeString(file(), original);

        VitiLedger ledger = store().initialize();

        assertEquals(
                new BigDecimal("0.123456789"),
                ledger.balance(id));

        assertEquals(
                VitiAmount.parse("50.000"),
                ledger.accounts().get(id).highest());

        assertEquals(original, Files.readString(file()));
    }

    @Test
    void samplePlaceholderIsPreservedButNeverRanked()
            throws Exception {
        String original = "players:\n  UUID-PEMAIN:\n"
                + "    name: Player1\n"
                + "    balance: 500.0\n"
                + "    highest_balance: 50000.0\n";

        Files.writeString(file(), original);

        VitiStore store = store();
        VitiLedger ledger = store.initialize();

        assertTrue(ledger.top().isEmpty());

        store.save(ledger);

        assertTrue(Files.readString(file()).contains("UUID-PEMAIN"));
    }

    @Test
    void firstMigrationBacksUpExactOriginalAndPreservesUnknownFields()
            throws Exception {
        String original = "# original comment\n"
                + "custom: keep\n"
                + "players:\n  " + id + ":\n"
                + "    name: One\n"
                + "    balance: 12.0\n"
                + "    custom-data:\n"
                + "      nested: preserve\n";

        Files.writeString(file(), original);

        VitiStore store = store();
        VitiLedger ledger = store.initialize();

        store.save(ledger.add(id, BigDecimal.ONE));

        assertEquals(
                original,
                Files.readString(
                        directory.resolve("viti.yml.before-remake")));

        var yaml = new YamlConfiguration();
        yaml.loadFromString(Files.readString(file()));

        assertEquals("keep", yaml.getString("custom"));

        assertEquals(
                "preserve",
                yaml.getString(
                        "players." + id + ".custom-data.nested"));

        assertTrue(
                yaml.get("players." + id + ".balance")
                        instanceof Number);

        assertEquals(
                "13",
                yaml.getString(
                        "players." + id + ".balance_exact"));

        store.save(ledger);

        assertEquals(
                original,
                Files.readString(
                        directory.resolve("viti.yml.before-remake")));
    }

    @Test
    void roundtripKeepsTransferNotesDeliveryAndReceipts()
            throws Exception {
        VitiStore store = store();

        UUID other = UUID.randomUUID();
        UUID issued = UUID.randomUUID();
        UUID old = UUID.randomUUID();

        VitiLedger state = store.initialize()
                .seed(id, "One", VitiAmount.parse("100"))
                .seed(other, "Two", BigDecimal.ZERO)
                .transfer(id, other, BigDecimal.TEN)
                .withdraw(id, issued, BigDecimal.TEN)
                .redeem(other, old, BigDecimal.ONE, true);

        store.save(state);

        assertEquals(state, store().load());

        VitiLedger ack = state.delivered(Set.of(issued));
        store.save(ack);

        assertEquals(ack, store().load());

        assertThrows(
                VitiLedger.Failure.class,
                () -> store().load().redeem(
                        id, old, BigDecimal.ONE, true));
    }

    @Test
    void exactFieldsRemainAuthoritativeOverDoubleMirrors()
            throws Exception {
        Files.writeString(
                file(),
                "players:\n  " + id + ":\n"
                        + "    name: One\n"
                        + "    balance: 0.30000000000000004\n"
                        + "    balance_exact: '0.3'\n");

        assertEquals(
                new BigDecimal("0.3"),
                store().load().balance(id));
    }

    @Test
    void invalidYamlAndSchemaAreNeverReplacedOnInitialize()
            throws Exception {
        for (String invalid : new String[]{
                "players: [broken\n",
                "players: wrong\n",
                "vitae-data-version: 2\n",
                "players:\n  not-a-uuid:\n    balance: 2\n",
                "players:\n  " + id + ":\n    balance: .nan\n",
                "players:\n  " + id + ":\n    balance: -1\n",
                "players:\n  " + id + ":\n    name: One\n",
                "vitae-redeemed: wrong\n"}) {
            Files.writeString(file(), invalid);

            assertThrows(
                    Exception.class,
                    () -> store().initialize());

            assertEquals(invalid, Files.readString(file()));
        }
    }

    @Test
    void failedReloadDoesNotReplaceSerializationBase()
            throws Exception {
        Files.writeString(file(), "custom: keep\nplayers: {}\n");

        VitiStore store = store();
        VitiLedger ledger = store.initialize();

        Files.writeString(file(), "players: [broken\n");

        assertThrows(
                InvalidConfigurationException.class,
                store::load);

        store.save(ledger);

        assertTrue(Files.readString(file()).contains("custom: keep"));
    }

    @Test
    void uppercaseLegacyUuidIsNotDuplicatedOnSave()
            throws Exception {
        String upper = id.toString()
                .toUpperCase(java.util.Locale.ROOT);

        Files.writeString(
                file(),
                "players:\n  " + upper + ":\n"
                        + "    name: One\n"
                        + "    balance: 1.0\n");

        VitiStore store = store();
        VitiLedger ledger = store.initialize();

        store.save(ledger.add(id, BigDecimal.ONE));

        assertEquals(1, store().load().accounts().size());
    }

    @Test
    void failedAtomicReplacementPreservesDestinationAndRemovesTemporaryFile()
            throws Exception {
        VitiStore store = store();

        VitiLedger ledger = store.initialize()
                .seed(id, "One", BigDecimal.TEN);

        store.save(ledger);

        Files.delete(file());
        Files.createDirectory(file());

        Path marker = file().resolve("keep");
        Files.writeString(marker, "original-data");

        assertThrows(
                java.io.IOException.class,
                () -> store.save(
                        ledger.add(id, BigDecimal.ONE)));

        assertEquals("original-data", Files.readString(marker));

        try (var paths = Files.list(directory)) {
            assertTrue(paths.noneMatch(
                    path -> path.getFileName()
                            .toString().endsWith(".tmp")));
        }
    }

    @Test
    void rejectsNoteWithRetiredIdOrUnknownOwnerWithoutRewriting()
            throws Exception {
        UUID serial = UUID.randomUUID();

        String accounts = "players:\n  " + id + ":\n"
                + "    name: One\n"
                + "    balance: 10\n";

        for (String value : new String[]{
                accounts
                        + "vitae-notes:\n  " + serial + ":\n"
                        + "    owner: " + id + "\n"
                        + "    amount: 1\n"
                        + "    pending: true\n"
                        + "vitae-redeemed:\n  - " + serial + "\n",
                accounts
                        + "vitae-notes:\n  " + serial + ":\n"
                        + "    owner: " + UUID.randomUUID() + "\n"
                        + "    amount: 1\n"
                        + "    pending: true\n",
                accounts
                        + "vitae-notes:\n  " + serial + ":\n"
                        + "    owner: " + id + "\n"
                        + "    amount: 1\n"
                        + "    pending: 'true'\n"}) {
            Files.writeString(file(), value);

            assertThrows(
                    IllegalArgumentException.class,
                    () -> store().initialize());

            assertEquals(value, Files.readString(file()));
        }
    }

    private Path file() {
        return directory.resolve("viti.yml");
    }

    private VitiStore store() {
        return new VitiStore(file());
    }
}