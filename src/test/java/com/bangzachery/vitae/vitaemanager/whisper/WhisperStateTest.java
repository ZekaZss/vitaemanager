package com.bangzachery.vitae.vitaemanager.whisper;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class WhisperStateTest {
    static final UUID WORLD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static final UUID PLAYER = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static WhisperDefinition ready(String id) {
        return WhisperDefinition.draft(id).withArea(WhisperArea.between(WORLD, 0, 64, 0, 2, 66, 2))
                .withTargets(Set.of(PLAYER)).withLines(List.of(new WhisperLine("Hei Lunar", "Datanglah", 40, 20, null)))
                .withEnabled(true);
    }
    @Test void separateIdsAndPlayersNeverShareCompletionAndDraftCannotTrigger() {
        var state = WhisperState.empty().put(ready("lorong")).put(ready("kuil"));
        state = state.finish(state.require("lorong"), PLAYER);
        assertFalse(state.eligible(state.require("lorong"), PLAYER));
        assertTrue(state.eligible(state.require("kuil"), PLAYER));
        assertFalse(state.eligible(state.require("kuil"), UUID.randomUUID()));
        assertEquals(1, state.count("lorong", PLAYER));
        assertEquals(0, state.count("kuil", PLAYER));
        assertFalse(state.eligible(WhisperDefinition.draft("draft"), PLAYER));
    }
    @Test void repeatLimitResetAndDeleteAreExplicitAndIndependent() {
        var d = ready("lorong").withLimit(2);
        var initial = WhisperState.empty().put(d).put(ready("kuil"));
        var one = initial.finish(d, PLAYER); assertTrue(one.eligible(d, PLAYER));
        var two = one.finish(d, PLAYER); assertFalse(two.eligible(d, PLAYER));
        assertThrows(IllegalArgumentException.class, () -> two.finish(d, PLAYER));
        assertEquals(initial, two.reset("lorong", PLAYER));
        assertEquals(initial, two.reset("lorong", null));
        var deleted = two.delete("lorong");
        assertEquals(0, deleted.count("lorong", PLAYER));
        assertTrue(deleted.definitions().containsKey("kuil"));
        assertEquals(0, deleted.create("lorong").count("lorong", PLAYER));
    }
    @Test void enabledDraftInvalidEditsAndStaleCompletionAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> WhisperDefinition.draft("draft").withEnabled(true));
        var d = ready("lorong"); var state = WhisperState.empty().put(d);
        assertThrows(IllegalArgumentException.class, () -> state.create("lorong"));
        assertThrows(IllegalArgumentException.class, () -> state.put(d.withEnabled(false)).finish(d, PLAYER));
        assertThrows(IllegalArgumentException.class, () -> d.withLines(List.of()));
        assertThrows(IllegalArgumentException.class, () -> d.withTargets(Set.of()));
        assertEquals(d, state.require("lorong"));
    }
    @Test void snapshotsAndTextsRemainImmutableAndLiteral() {
        var targets = new HashSet<>(Set.of(PLAYER)); var lines = new ArrayList<>(ready("x").lines());
        var d = new WhisperDefinition("x", ready("x").area(), targets, lines, 1, true);
        targets.clear(); lines.clear(); assertEquals(1, d.targets().size()); assertEquals(1, d.lines().size());
        assertThrows(UnsupportedOperationException.class, () -> d.targets().clear());
        assertThrows(UnsupportedOperationException.class, () -> WhisperState.empty().put(d).definitions().clear());
        assertEquals("<red>tetap literal", new WhisperLine("<red>tetap literal", "", 20, 0, null).title());
    }
    @Test void malformedIdsTextTimesAndSoundAreRejected() {
        for (String id : List.of("", "UpperCase", "a.b", "a/b", "x".repeat(49)))
            assertThrows(IllegalArgumentException.class, () -> WhisperDefinition.draft(id));
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine("", "", 20, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine("a\nb", "", 20, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine("a", "", 19, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine("a", "", 20, -1, null));
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine.Audio("bad key", 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine.Audio("vitae:voice", Float.NaN, 1));
        assertThrows(IllegalArgumentException.class, () -> new WhisperLine.Audio("vitae:voice", 0.3f, 2.1f));
    }
}