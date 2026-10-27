package com.bangzachery.vitae.vitaemanager.magic;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SpellAudioTest {
    @Test void deliveryFailureClosingAudioStopsRemainingCuesImmediately() throws Exception {
        for (String failedPhase : List.of("activation", "active", "tick")) {
            var config = new YamlConfiguration();
            config.loadFromString("sounds:\n  activation:\n    cues:\n      - {type: custom, id: pack:start}\n      - {type: custom, id: pack:start2}\n  active:\n    interval-ticks: 1\n    cues:\n      - {type: custom, id: pack:loop}\n      - {type: custom, id: pack:loop2}\n");
            List<String> played = new ArrayList<>();
            SpellAudio[] ref = new SpellAudio[1];
            boolean[] ticking = {false};
            ref[0] = new SpellAudio(SpellSoundPlan.parse(config, new YamlConfiguration()), cue -> {
                played.add(cue.id());
                if (failedPhase.equals("activation") || failedPhase.equals("active") && cue.id().equals("pack:loop")
                        || failedPhase.equals("tick") && ticking[0]) ref[0].finish(false);
            });
            ref[0].start("active", 0);
            if (failedPhase.equals("tick")) {
                played.clear(); ticking[0] = true; ref[0].tick(1);
            }
            assertTrue(ref[0].closed());
            assertEquals(failedPhase.equals("active") ? 3 : 1, played.size(), failedPhase);
            ref[0].tick(100); assertTrue(ref[0].closed());
        }
    }

    private SpellSoundPlan plan() throws Exception {
        var yaml = new YamlConfiguration();
        yaml.loadFromString("""
                sounds:
                  activation:
                    cues:
                      - {type: custom, id: pack:threat}
                      - {type: custom, id: pack:rumble}
                  formation:
                    interval-ticks: 40
                    cues:
                      - {type: custom, id: pack:forming}
                  active:
                    interval-ticks: 50
                    audience: victims
                    origin: victim
                    range: 20
                    cues:
                      - {type: custom, id: pack:bat}
                  release:
                    cues:
                      - {type: custom, id: pack:release}
                  ending:
                    cues:
                      - {type: custom, id: pack:end}
                """);
        return SpellSoundPlan.parse(yaml, new YamlConfiguration());
    }

    @Test void transitionStopsOldRepeatAndEndingRunsOnlyOnce() throws Exception {
        List<String> played = new ArrayList<>();
        var audio = new SpellAudio(plan(), c -> played.add(c.id()));
        audio.start("formation", 100);
        audio.start("formation", 101);
        assertEquals(List.of("pack:threat", "pack:rumble", "pack:forming"), played);
        audio.tick(139); assertEquals(3, played.size());
        audio.tick(140); assertEquals("pack:forming", played.getLast());
        audio.phase("active", 141);
        audio.tick(180); assertEquals(1, Collections.frequency(played, "pack:bat"));
        audio.tick(191); assertEquals(2, Collections.frequency(played, "pack:bat"));
        audio.phase("release", 192);
        audio.tick(10000); assertEquals(2, Collections.frequency(played, "pack:bat"));
        audio.finish(true); audio.finish(true); audio.tick(11000);
        assertEquals(1, Collections.frequency(played, "pack:end"));
        assertTrue(audio.closed());
    }

    @Test void disableOrRejectedUseCannotLeaveRepeatsOrClosingAudio() throws Exception {
        List<String> played = new ArrayList<>();
        var rejected = new SpellAudio(plan(), c -> played.add(c.id()));
        rejected.finish(true); assertTrue(played.isEmpty());
        var audio = new SpellAudio(plan(), c -> played.add(c.id()));
        audio.start("active", 0); int before = played.size();
        audio.finish(false); audio.phase("release", 10); audio.tick(1000);
        assertEquals(before, played.size());
    }

    @Test void missedTicksDoNotCreateCatchUpSoundBursts() throws Exception {
        List<String> played = new ArrayList<>();
        var audio = new SpellAudio(plan(), c -> played.add(c.id()));
        audio.start("active", 0); audio.tick(10000);
        assertEquals(2, Collections.frequency(played, "pack:bat"));
        audio.tick(10001); assertEquals(2, Collections.frequency(played, "pack:bat"));
    }

    @Test void legacyCustomCueOverridesNewDefaultsAndDisabledPhaseStaysDisabled() throws Exception {
        var defaults = new YamlConfiguration(); defaults.loadFromString("sounds:\n  active:\n    cues:\n      - {id: ENTITY_WARDEN_HEARTBEAT}\n  ending:\n    cues:\n      - {id: BLOCK_BEACON_DEACTIVATE}\n");
        var yaml = new YamlConfiguration(); yaml.loadFromString("sounds:\n  active:\n    type: custom\n    id: itemsadder:old_sound\n    volume: 0.4\n    pitch: 0.7\n    interval-ticks: 51\n    range: 14\n    audience: owner\n  ending:\n    enabled: false\n");
        var plan = SpellSoundPlan.parse(yaml, defaults);
        var cue = plan.cues("active").getFirst();
        assertEquals("itemsadder:old_sound", cue.id());
        assertEquals(51, cue.intervalTicks()); assertEquals(14, cue.range());
        assertEquals(SpellSoundPlan.Audience.OWNER, cue.audience());
        assertTrue(plan.cues("ending").isEmpty());
        assertEquals(0.4, yaml.getDouble("sounds.active.volume")); // parsing does not rewrite configuration
    }

    @Test void invalidSoundOrAudienceAndNonfiniteNumbersFailBeforeActivation() {
        var yaml = new YamlConfiguration();
        for (Object invalid : List.of(Double.NaN, Double.POSITIVE_INFINITY, -1)) {
            yaml.set("sounds.active.volume", invalid);
            assertThrows(IllegalArgumentException.class, () -> SpellSoundPlan.parse(yaml, new YamlConfiguration()));
        }
        yaml.set("sounds.active.volume", 1);
        yaml.set("sounds.active.audience", "whole-server");
        assertThrows(IllegalArgumentException.class, () -> SpellSoundPlan.parse(yaml, new YamlConfiguration()));
    }

    @Test void repeatingSoundsStopOnPhaseExitWithoutCuttingAnotherVitaeUse() {
        Map<SpellSoundDelivery.Key, Set<SpellSoundDelivery>> registry = new HashMap<>();
        List<SpellSoundDelivery.Key> stopped = new ArrayList<>();
        var a = new SpellSoundDelivery(registry, stopped::add);
        var b = new SpellSoundDelivery(registry, stopped::add);
        var key = new SpellSoundDelivery.Key(UUID.randomUUID(), "pack:pressure");
        a.rememberLoop(key); a.rememberLoop(key); b.rememberLoop(key);
        a.stopRepeats(); assertTrue(stopped.isEmpty());
        b.stopRepeats(); b.stopRepeats();
        assertEquals(List.of(key), stopped); assertTrue(registry.isEmpty());
    }

    @Test void phaseAndDisableInvokeLoopCleanupOnlyOncePerTransition() throws Exception {
        int[] stopped = {0};
        var audio = new SpellAudio(plan(), c -> {}, () -> stopped[0]++);
        audio.start("formation", 0); audio.phase("formation", 20);
        assertEquals(1, stopped[0]);
        audio.phase("release", 30); assertEquals(2, stopped[0]);
        audio.finish(false); audio.finish(false); assertEquals(3, stopped[0]);
    }

    @Test void audiencesAndRangeNeverSelectWholeServer() {
        var cue = new SpellSoundPlan.Cue("custom", "pack:id", 1, 1, 50,
                SpellSoundPlan.Audience.VICTIMS, SpellSoundPlan.Origin.VICTIM, 20);
        assertTrue(SpellSoundDelivery.audible(cue, false, true, true, 400));
        assertFalse(SpellSoundDelivery.audible(cue, false, true, true, 401));
        assertFalse(SpellSoundDelivery.audible(cue, true, false, true, 0));
        assertFalse(SpellSoundDelivery.audible(cue, false, true, false, 0));
    }

    @Test void bundledPresetsUseRealPaperVanillaFieldsAndKeepBatOnBothFloatingPhases() throws Exception {
        for (String id : List.of("unlimited_misery", "telekinesis", "tunduk")) {
            var yaml = new YamlConfiguration();
            try (var in = getClass().getResourceAsStream("/magic/" + id + ".yml")) {
                assertNotNull(in); yaml.load(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
            var plan = SpellSoundPlan.parse(yaml, new YamlConfiguration());
            for (String phase : List.of("activation", "active", "release", "ending")) assertFalse(plan.cues(phase).isEmpty());
            for (String phase : SpellSoundPlan.PHASES) for (var cue : plan.cues(phase)) {
                assertEquals("vanilla", cue.type());
                assertNotNull(org.bukkit.Sound.class.getField(cue.id()));
            }
            if (id.equals("telekinesis")) for (String phase : List.of("formation", "active"))
                assertTrue(plan.cues(phase).stream().anyMatch(c -> c.id().equals("ENTITY_BAT_AMBIENT") && c.intervalTicks() == 50));
        }
    }

    @Test void departingVictimRetiresSourceAndReceiverWithoutCuttingOtherSources() {
        Map<SpellSoundDelivery.Key, Set<SpellSoundDelivery>> registry = new HashMap<>();
        List<SpellSoundDelivery.Key> stopped = new ArrayList<>();
        var delivery = new SpellSoundDelivery(registry, stopped::add);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), observer = UUID.randomUUID();
        var observerKey = new SpellSoundDelivery.Key(observer, "pack:pressure");
        var ownKey = new SpellSoundDelivery.Key(a, "pack:pressure");
        delivery.rememberLoop(observerKey, a); delivery.rememberLoop(observerKey, b);
        delivery.rememberLoop(ownKey, a);
        delivery.releaseVictim(a);
        assertEquals(List.of(ownKey), stopped);
        delivery.releaseVictim(b);
        assertEquals(Set.of(ownKey, observerKey), new HashSet<>(stopped));
        assertTrue(registry.isEmpty());
    }

    @Test void stoppingOneFailedClientStillRetiresAllLoopOwnership() {
        Map<SpellSoundDelivery.Key, Set<SpellSoundDelivery>> registry = new HashMap<>();
        int[] calls = {0};
        var delivery = new SpellSoundDelivery(registry, key -> { calls[0]++; throw new IllegalStateException("offline client"); });
        delivery.rememberLoop(new SpellSoundDelivery.Key(UUID.randomUUID(), "pack:a"));
        delivery.rememberLoop(new SpellSoundDelivery.Key(UUID.randomUUID(), "pack:b"));
        assertThrows(IllegalStateException.class, delivery::stopRepeats);
        assertEquals(2, calls[0]); assertTrue(registry.isEmpty());
        assertDoesNotThrow(delivery::stopRepeats);
    }
    @Test void malformedSectionsAreRejectedInsteadOfReplacingUserChoiceWithDefaults() {
        var defaults = new YamlConfiguration(); defaults.set("sounds.active.id", "ENTITY_BAT_AMBIENT");
        for (Object value : List.of("disabled", List.of("sound"), 12)) {
            var yaml = new YamlConfiguration(); yaml.set("sounds.active", value);
            assertThrows(IllegalArgumentException.class, () -> SpellSoundPlan.parse(yaml, defaults));
            var malformedRoot = new YamlConfiguration(); malformedRoot.set("sounds", value);
            assertThrows(IllegalArgumentException.class, () -> SpellSoundPlan.parse(malformedRoot, defaults));
        }
    }
}
