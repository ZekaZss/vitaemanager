package com.bangzachery.vitae.vitaemanager.totem;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class TotemLimitStoreTest {
    @TempDir Path directory;
    private Path file() { return directory.resolve("totem-settings.yml"); }
    private TotemLimitStore store() { return new TotemLimitStore(file()); }

    @Test void absentOverrideUsesConfigWithoutCreatingFile() throws Exception {
        assertNull(store().load());
        assertFalse(Files.exists(file()));
    }

    @Test void roundTripLimitSurvivesNewStoreInstanceAndReplacesAtomically() throws Exception {
        store().save(5);
        assertEquals(5, store().load());
        store().save(2);
        assertEquals(2, store().load());
        assertNoTemporaryFiles();
    }

    @Test void malformedYamlSchemaAndTypesAreNeverRewritten() throws Exception {
        for (String text : new String[]{"[broken\n", "state-version: 2\nlimit: 3\n",
                "state-version: 1\nlimit: '3'\n", "state-version: 1\nlimit: 3.0\n",
                "state-version: 1\nlimit: 0\n", "state-version: 1\nlimit: 1001\n", "limit: 3\n"}) {
            Files.writeString(file(), text);
            assertThrows(Exception.class, () -> store().load());
            assertEquals(text, Files.readString(file()));
        }
    }

    @Test void invalidSaveKeepsPreviousOverrideBytes() throws Exception {
        store().save(3);
        String original = Files.readString(file());
        assertThrows(IllegalArgumentException.class, () -> store().save(0));
        assertEquals(original, Files.readString(file()));
    }

    @Test void failedReplacementKeepsDestinationAndRemovesTemporaryFile() throws Exception {
        Files.createDirectory(file());
        Path marker = file().resolve("keep");
        Files.writeString(marker, "original");
        assertThrows(java.io.IOException.class, () -> store().save(4));
        assertEquals("original", Files.readString(marker));
        assertNoTemporaryFiles();
    }

    private void assertNoTemporaryFiles() throws Exception {
        try (var paths = Files.list(directory)) {
            assertTrue(paths.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}