package com.bangzachery.vitae.vitaemanager.whisper;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperCinematicStoreTest {
    @TempDir Path directory;
    private Path file() { return directory.resolve("whispers.yml"); }
    private WhisperStore store() { return new WhisperStore(file(), Set.of("minecraft:music_disc.11", "minecraft:block.wood.break", "minecraft:entity.ghast.scream", "minecraft:entity.warden.ambient")); }
    @Test void oldDataLoadsWithoutRewritingBytesOrLosingCompletedPlayers() throws Exception {
        var definition = WhisperStateTest.ready("old");
        var state = WhisperState.empty().put(definition).finish(definition, WhisperStateTest.PLAYER); store().save(state);
        var yaml = YamlConfiguration.loadConfiguration(file().toFile());
        yaml.set("whispers.old.visual", null); yaml.set("whispers.old.music", null);
        Files.writeString(file(), yaml.saveToString()); byte[] original = Files.readAllBytes(file());
        assertEquals(state, store().load()); assertArrayEquals(original, Files.readAllBytes(file()));
    }
    @Test void colouredTemplatesMusicTypingAndCompletedPlayersSurviveReload() throws Exception {
        var base = WhisperStateTest.ready("lunar");
        var state = WhisperState.empty().put(base).finish(base, WhisperStateTest.PLAYER);
        var preset = WhisperTemplates.apply(base, "lunar").withEnabled(true);
        state = state.put(preset); store().save(state);
        assertEquals(state, store().load());
        assertEquals("&9Lunar", store().load().require("lunar").lines().getFirst().title());
        assertEquals(1, store().load().count("lunar", WhisperStateTest.PLAYER));
    }
    @Test void badMusicAndVisualSettingsCannotOverwritePreviousBytes() throws Exception {
        var definition = WhisperTemplates.apply(WhisperStateTest.ready("a"), "lunar");
        store().save(WhisperState.empty().put(definition)); String original = Files.readString(file());
        for (var change : List.of(Map.entry("whispers.a.music.repeat-ticks", (Object) 1), Map.entry("whispers.a.music.volume", (Object) 1.1),
                Map.entry("whispers.a.music.key", (Object) "minecraft:does.not.exist"), Map.entry("whispers.a.visual.typing", (Object) "true"),
                Map.entry("whispers.a.visual.characters-per-tick", (Object) 9), Map.entry("whispers.a.visual.fade-in-ticks", (Object) (-1)))) {
            var yaml = new YamlConfiguration(); yaml.loadFromString(original); yaml.set(change.getKey(), change.getValue());
            Files.writeString(file(), yaml.saveToString()); byte[] bad = Files.readAllBytes(file());
            assertThrows(Exception.class, () -> store().load()); assertArrayEquals(bad, Files.readAllBytes(file()));
        }
    }
    @Test void unknownVanillaMusicIsRefusedBeforeDiskReplacementButCustomMusicIsAllowed() throws Exception {
        var store = store(); store.initialize(); byte[] original = Files.readAllBytes(file());
        var definition = WhisperStateTest.ready("a").withMusic(new WhisperMusic(new WhisperLine.Audio("minecraft:wrong", .5f, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> store.save(WhisperState.empty().put(definition)));
        assertArrayEquals(original, Files.readAllBytes(file()));
        var custom = definition.withMusic(new WhisperMusic(new WhisperLine.Audio("vitae:horor/lunar", .5f, 1), 200));
        var state = WhisperState.empty().put(custom); store.save(state); assertEquals(state, store.load());
    }
}
