package com.bangzachery.vitae.vitaemanager.whisper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperStoreTest {
    @TempDir Path directory;
    private Path file() { return directory.resolve("whispers.yml"); }
    private WhisperStore store() { return new WhisperStore(file(), Set.of("minecraft:block.note_block.hat")); }
    @Test void freshFileIsEmptyAndSeparateDefinitionsAndReceiptsSurviveRestart() throws Exception {
        var store = store(); assertEquals(WhisperState.empty(), store.initialize());
        var a = WhisperStateTest.ready("a"); var b = WhisperStateTest.ready("b");
        var state = WhisperState.empty().put(a).put(b).finish(a, WhisperStateTest.PLAYER);
        store.save(state); assertEquals(state, store().initialize());
        assertEquals(0, store().load().count("b", WhisperStateTest.PLAYER));
        store.save(state.reset("a", WhisperStateTest.PLAYER));
        assertTrue(store().load().eligible(a, WhisperStateTest.PLAYER));
    }
    @Test void customAndVanillaAudioRoundTripWithLiteralUnicodeAndDrafts() throws Exception {
        var a = WhisperStateTest.ready("a").withLines(List.of(
                new WhisperLine("Hei Lunar — ☾", "<red>literal", 30, 10, new WhisperLine.Audio("vitae:whisper/line_1", 0.3f, 1)),
                new WhisperLine("", "jangan pergi", 20, 0, new WhisperLine.Audio("minecraft:block.note_block.hat", 0, 2))));
        var state = WhisperState.empty().put(a).create("draft"); store().save(state);
        assertEquals(state, store().load());
    }
    @Test void malformedSchemaIsRefusedWithoutTouchingExistingBytes() throws Exception {
        var valid = WhisperState.empty().put(WhisperStateTest.ready("a")); store().save(valid);
        String original = Files.readString(file());
        List<String> invalid = List.of("[broken", "state-version: 2\nwhispers: {}\n",
                original.replace("plays: 1", "plays: '1'"), original.replace("enabled: true", "enabled: maybe"),
                original.replace("duration-ticks: 40", "duration-ticks: 0"),
                original.replace(WhisperStateTest.PLAYER.toString(), "not-uuid"),
                original.replace("completed: {}", "completed: []"));
        for (String text : invalid) {
            assertNotEquals(original, text); Files.writeString(file(), text);
            assertThrows(Exception.class, () -> store().initialize()); assertEquals(text, Files.readString(file()));
        }
    }
    @Test void allLineEndingStylesRemainReadable() throws Exception {
        var state = WhisperState.empty().put(WhisperStateTest.ready("a")); store().save(state);
        String source = Files.readString(file()).replace("\r\n", "\n").replace("\r", "\n");
        for (String newline : List.of("\n", "\r\n", "\r")) {
            Files.writeString(file(), source.replace("\n", newline)); assertEquals(state, store().load());
        }
    }
    @Test void missingFileReloadAndAtomicReplacementFailureNeverDestroyPreviousData() throws Exception {
        var store = store(); store.initialize(); Files.delete(file());
        assertThrows(java.io.IOException.class, store::load);
        Files.createDirectory(file()); Files.writeString(file().resolve("keep"), "safe");
        assertThrows(java.io.IOException.class, () -> store.save(WhisperState.empty()));
        assertEquals("safe", Files.readString(file().resolve("keep")));
        try (var paths = Files.list(directory)) { assertTrue(paths.noneMatch(path -> path.toString().endsWith(".tmp"))); }
    }
    @Test void unknownVanillaSoundCannotOverwriteAValidFile() throws Exception {
        var store = store(); store.initialize(); String original = Files.readString(file());
        var definition = WhisperStateTest.ready("a").withLines(List.of(new WhisperLine("a", "", 20, 0,
                new WhisperLine.Audio("minecraft:does.not.exist", 1, 1))));
        assertThrows(IllegalArgumentException.class, () -> store.save(WhisperState.empty().put(definition)));
        assertEquals(original, Files.readString(file()));
    }
}