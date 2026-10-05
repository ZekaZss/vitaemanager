package com.bangzachery.vitae.vitaemanager.whisper;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperPlaybackTest {
    private final UUID player = WhisperStateTest.PLAYER;
    @Test void fullSequenceHonorsDisplayAndGapThenWaitsForDurableReceipt() {
        var lines = List.of(new WhisperLine("one", "", 40, 20, null), new WhisperLine("two", "", 20, 120, null));
        var run = new WhisperPlayback.Run(WhisperStateTest.ready("a").withLines(lines), false);
        var playback = new WhisperPlayback(); assertTrue(playback.enqueue(player, run));
        assertEquals("one", playback.advance(player, 10).line().title());
        assertNull(playback.advance(player, 49)); assertNull(playback.advance(player, 69));
        assertEquals("two", playback.advance(player, 70).line().title());
        assertNull(playback.advance(player, 89)); assertTrue(playback.advance(player, 90).complete());
        assertNull(playback.advance(player, 1000)); assertEquals(run, playback.head(player));
        assertFalse(playback.enqueue(player, new WhisperPlayback.Run(run.definition(), false)));
        playback.acknowledge(player, run); assertTrue(playback.players().isEmpty());
    }
    @Test void overlappingAreasQueueWithoutReplacingTitlesAndPlayersAreIndependent() {
        var playback = new WhisperPlayback();
        var a = new WhisperPlayback.Run(WhisperStateTest.ready("a"), false);
        var b = new WhisperPlayback.Run(WhisperStateTest.ready("b"), false);
        var other = UUID.randomUUID();
        assertTrue(playback.enqueue(player, a)); assertTrue(playback.enqueue(player, b));
        assertTrue(playback.enqueue(other, b)); assertEquals(a, playback.advance(player, 0).run());
        assertEquals(b, playback.advance(other, 0).run());
        assertTrue(playback.advance(player, 40).complete()); assertNull(playback.advance(player, 100));
        playback.acknowledge(player, a); assertEquals(b, playback.advance(player, 101).run());
    }
    @Test void cancellationAndLateCallbacksDoNotEraseReplacementOrUnrelatedQueue() {
        var playback = new WhisperPlayback(); var a = new WhisperPlayback.Run(WhisperStateTest.ready("a"), false);
        var b = new WhisperPlayback.Run(WhisperStateTest.ready("b"), true);
        playback.enqueue(player, a); playback.enqueue(player, b); playback.advance(player, 0);
        assertTrue(playback.cancel(player, "a"));
        playback.acknowledge(player, a); assertEquals(b, playback.head(player));
        assertTrue(playback.advance(player, 1).run().preview());
        playback.clear(player); assertNull(playback.head(player));
        var replacement = new WhisperPlayback.Run(a.definition(), false);
        playback.enqueue(player, replacement); playback.acknowledge(player, a);
        assertEquals(replacement, playback.head(player));
    }
    @Test void queueIsBoundedAndEmptyDraftCannotReserveAPlayer() {
        var playback = new WhisperPlayback();
        assertFalse(playback.enqueue(player, new WhisperPlayback.Run(WhisperDefinition.draft("empty"), true)));
        assertTrue(playback.players().isEmpty());
        for (int i = 0; i < 32; i++) assertTrue(playback.enqueue(player, new WhisperPlayback.Run(WhisperStateTest.ready("id_" + i), false)));
        assertFalse(playback.enqueue(player, new WhisperPlayback.Run(WhisperStateTest.ready("overflow"), false)));
        assertFalse(playback.cancel(player, "id_31"));
        assertTrue(playback.enqueue(player, new WhisperPlayback.Run(WhisperStateTest.ready("overflow"), false)));
    }
    @Test void lagDoesNotSkipLinesAndZeroGapStillShowsEveryPage() {
        var definition = WhisperStateTest.ready("a").withLines(List.of(new WhisperLine("one", "", 20, 0, null), new WhisperLine("two", "", 20, 0, null)));
        var playback = new WhisperPlayback(); playback.enqueue(player, new WhisperPlayback.Run(definition, false));
        assertEquals("one", playback.advance(player, 0).line().title());
        assertEquals("two", playback.advance(player, 500).line().title());
        assertNull(playback.advance(player, 519)); assertTrue(playback.advance(player, 520).complete());
    }
}