package com.bangzachery.vitae.vitaemanager.whisper;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperCinematicTest {
    @Test void coloursAreRenderedWithoutTurningMiniMessageIntoInstructions() {
        var serializer = LegacyComponentSerializer.builder().character('&').hexColors().build();
        assertEquals("Lunar", WhisperText.plain("&9Lunar"));
        assertEquals("Lunar", WhisperText.plain("§9Lunar"));
        assertEquals("Lunar", WhisperText.plain("&A&LLunar"));
        assertEquals("Biru", WhisperText.plain("&#55AAFFBiru"));
        assertTrue(serializer.serialize(WhisperText.component("&9Lunar", NamedTextColor.WHITE)).contains("&9Lunar"));
        assertEquals("<red>tetap literal", WhisperText.plain("<red>tetap literal"));
        assertEquals(NamedTextColor.WHITE, WhisperText.component("tanpa warna", NamedTextColor.WHITE).color());
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine("&9&l", "", 20, 0, null));
    }
    @Test void typingPrefixDoesNotCutColourCodesOrUnicodeCharacters() {
        String source = "&#55AAFF&lLunar 🌙 datang";
        assertEquals("Lunar 🌙", WhisperText.plain(WhisperText.prefix(source, 7)));
        assertEquals("", WhisperText.prefix(source, 0));
        assertEquals(source, WhisperText.prefix(source, 100));
        assertEquals(14, WhisperText.length(source));
    }
    @Test void longDialogSplitsAtWordsAndCarriesColourAndStyle() {
        String text = "&c&lKali ini kau harus berhati-hati dan beri tahu kepada rekan Revenant-mu bahwa tempat ini bukan dungeon seperti sebelumnya.";
        var pages = WhisperText.pages(text);
        assertTrue(pages.size() > 1);
        assertEquals(WhisperText.plain(text), String.join(" ", pages.stream().map(WhisperText::plain).toList()));
        for (String page : pages) {
            assertTrue(page.startsWith("&c&l"));
            assertTrue(WhisperText.length(page) <= WhisperText.PAGE_WIDTH);
            assertTrue(page.codePointCount(0, page.length()) <= 160);
            assertDoesNotThrow(() -> new WhisperLine("", page, 80, 10, null));
        }
    }
    @Test void longWordsUnicodeAndHexPagesHaveNoLostCharacters() {
        String text = "&#33AAFF" + "🌙".repeat(120);
        var pages = WhisperText.pages(text);
        assertEquals("🌙".repeat(120), String.join("", pages.stream().map(WhisperText::plain).toList()));
        pages.forEach(page -> assertTrue(page.startsWith("&#33AAFF")));
        for (String invalid : List.of("", "&c&l", "a\nb", "a\u2028b", "x".repeat(4001)))
            assertThrows(IllegalArgumentException.class, () -> WhisperText.pages(invalid));
    }
    @Test void templatesKeepAreaReceiversAndCompletedProgressAndRequireEnable() {
        var original = WhisperStateTest.ready("dungeon");
        var finished = WhisperState.empty().put(original).finish(original, WhisperStateTest.PLAYER);
        for (String name : WhisperTemplates.names()) {
            var preset = WhisperTemplates.apply(original, name);
            assertEquals(original.area(), preset.area()); assertEquals(original.targets(), preset.targets());
            assertEquals(original.id(), preset.id()); assertEquals(1, preset.limit()); assertFalse(preset.enabled());
            assertTrue(preset.visual().typing()); assertNotNull(preset.music());
            assertTrue(preset.lines().stream().allMatch(line -> line.audio() != null));
            var changed = finished.put(preset.withEnabled(true));
            assertEquals(1, changed.count(original.id(), WhisperStateTest.PLAYER));
            assertFalse(changed.eligible(changed.require(original.id()), WhisperStateTest.PLAYER));
        }
        assertEquals("&9Lunar", WhisperTemplates.apply(original, "lunar").lines().getFirst().title());
    }
    @Test void metadataSurvivesEveryLegacyDefinitionEdit() {
        var music = new WhisperMusic(new WhisperLine.Audio("vitae:whisper", .5f, .8f), 200);
        var visual = new WhisperVisual(true, 1, 10, 15);
        var source = WhisperStateTest.ready("a").withMusic(music).withVisual(visual);
        for (var edited : List.of(source.withArea(source.area()), source.withTargets(source.targets()), source.withLines(source.lines()), source.withLimit(2), source.withEnabled(false))) {
            assertEquals(music, edited.music()); assertEquals(visual, edited.visual());
        }
        var legacy = new WhisperDefinition("old", source.area(), source.targets(), source.lines(), 1, true);
        assertNull(legacy.music()); assertEquals(WhisperVisual.defaults(), legacy.visual());
        assertThrows(IllegalArgumentException.class, () -> new WhisperMusic(music.sound(), 1));
        assertThrows(IllegalArgumentException.class, () -> new WhisperVisual(true, 0, 5, 5));
    }
    @Test void durationAndQueueMatchTheLastLineWithoutAnExtraFinalGap() {
        var definition = WhisperStateTest.ready("a").withLines(List.of(new WhisperLine("a", "", 40, 20, null), new WhisperLine("b", "", 60, 100, null)));
        assertEquals(120, definition.durationTicks());
        var queue = new WhisperPlayback(); var run = new WhisperPlayback.Run(definition, true);
        assertTrue(queue.enqueue(WhisperStateTest.PLAYER, run));
        assertNotNull(queue.advance(WhisperStateTest.PLAYER, 1));
        assertNull(queue.advance(WhisperStateTest.PLAYER, 60));
        assertEquals("b", queue.advance(WhisperStateTest.PLAYER, 61).line().title());
        assertNull(queue.advance(WhisperStateTest.PLAYER, 120));
        assertTrue(queue.advance(WhisperStateTest.PLAYER, 121).complete());
    }
}
