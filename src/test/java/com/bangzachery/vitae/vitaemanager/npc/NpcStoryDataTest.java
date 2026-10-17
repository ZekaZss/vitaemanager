package com.bangzachery.vitae.vitaemanager.npc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcStoryDataTest {
    @TempDir Path directory;
    Definition npc() {
        var json = NpcStore.JSON.toJsonTree(NpcGateDataTest.npc("guard")).getAsJsonObject();
        json.add("nodes", NpcStore.JSON.toJsonTree(Map.of("start", new Node("Bantu aku?", List.of(
                new Choice("Ya", "END", "sikap", "membantu"), new Choice("Tidak", "END", "sikap", "menolak"))),
                "ramah", new Node("Terima kasih", List.of()), "dingin", new Node("Kamu menolak", List.of()))));
        json.add("memory", NpcStore.JSON.toJsonTree(List.of(new MemoryRule("sikap", "membantu", "ramah"), new MemoryRule("sikap", "menolak", "dingin"))));
        json.add("gift", NpcStore.JSON.toJsonTree(new Reward(new BigDecimal("100"), List.of())));
        return NpcStore.JSON.fromJson(json, Definition.class);
    }
    @Test void legacyNpcMigratesOnceWithExactBackupAndKeepsIdentityProgressQuotaAndGift() throws Exception {
        Definition npc = npc().withGate(NpcGateDataTest.gate(GateStatus.OPEN)); UUID player = UUID.randomUUID();
        State state = State.empty().put(npc).finish(player, npc, false, UUID.randomUUID(), 100);
        var json = NpcStore.JSON.toJsonTree(state).getAsJsonObject(); json.remove("memories");
        var record = json.getAsJsonObject("npcs").getAsJsonObject("guard"); record.remove("appearance"); record.remove("memory");
        record.getAsJsonObject("gate").remove("timing");
        for (var entry : record.getAsJsonObject("nodes").entrySet()) for (var choice : entry.getValue().getAsJsonObject().getAsJsonArray("choices")) {
            choice.getAsJsonObject().remove("memoryKey"); choice.getAsJsonObject().remove("memoryValue");
        }
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(json)); byte[] old = Files.readAllBytes(file);
        NpcStore store = new NpcStore(file); State loaded = store.initialize();
        assertEquals(npc.token(), loaded.npcs().get("guard").token()); assertEquals(state.completed(), loaded.completed());
        assertEquals(state.pending(), loaded.pending()); assertEquals(state.quotas(), loaded.quotas());
        assertTrue(loaded.npcs().get("guard").appearance().nametag()); assertTrue(loaded.npcs().get("guard").appearance().track());
        assertEquals(30, loaded.npcs().get("guard").gate().timing().seconds());
        List<Path> backups; try (var files = Files.list(directory)) { backups = files.filter(p -> p.toString().endsWith(".bak")).toList(); }
        assertEquals(1, backups.size()); assertArrayEquals(old, Files.readAllBytes(backups.getFirst()));
        assertEquals(loaded, store.initialize()); try (var files = Files.list(directory)) { assertEquals(1, files.filter(p -> p.toString().endsWith(".bak")).count()); }
    }
    @Test void playersAndNpcsKeepSeparateMemoriesAcrossRestartAndRewardAcknowledgement() throws Exception {
        Definition npc = npc(); UUID yes = UUID.randomUUID(), no = UUID.randomUUID();
        State state = State.empty().put(npc).remember(yes, npc, npc.nodes().get("start").choices().get(0))
                .remember(no, npc, npc.nodes().get("start").choices().get(1)).finish(yes, npc, false, UUID.randomUUID(), 100);
        assertEquals("ramah", state.recalledRoot(yes, npc)); assertEquals("dingin", state.recalledRoot(no, npc));
        state = state.acknowledge(yes, state.pending().get(yes).receipt()); assertEquals("ramah", state.recalledRoot(yes, npc));
        State secondVisit = state.finish(yes, npc, false, UUID.randomUUID(), 101); assertTrue(secondVisit.pending().isEmpty());
        var store = new NpcStore(directory.resolve("npc.json")); store.save(secondVisit); assertEquals(secondVisit, store.initialize());
        Definition other = NpcGateDataTest.npc("other"); assertTrue(secondVisit.memory(yes, other).isEmpty());
    }
    @Test void gateCyclesAndRenamesRetainMemoriesAndOnceOnlyGift() {
        Definition npc = npc().withGate(NpcGateDataTest.gate(GateStatus.CLOSED)); UUID player = UUID.randomUUID();
        State state = State.empty().put(npc).remember(player, npc, npc.nodes().get("start").choices().getFirst()).finish(player, npc, false, UUID.randomUUID(), 100);
        state = state.acknowledge(player, state.pending().get(player).receipt());
        for (int i = 0; i < 3; i++) {
            state = state.put(state.npcs().get("guard").withGate(npc.gate().opened(1000 + i * 40_000)));
            state = state.put(state.npcs().get("guard").withGate(npc.gate().withStatus(GateStatus.CLOSED)));
            state = state.finish(player, state.npcs().get("guard"), false, UUID.randomUUID(), 100);
        }
        var json = NpcStore.JSON.toJsonTree(state.npcs().get("guard")).getAsJsonObject(); json.addProperty("name", "&6Penjaga &eGerbang");
        var renamed = NpcStore.JSON.fromJson(json, Definition.class); state = state.put(renamed);
        assertTrue(state.done(player, renamed)); assertTrue(state.pending().isEmpty()); assertEquals("ramah", state.recalledRoot(player, renamed));
    }
    @Test void resettingOneNpcDoesNotEraseOtherPlayersMemories() {
        Definition npc = npc(); UUID first = UUID.randomUUID(), second = UUID.randomUUID(); Choice choice = npc.nodes().get("start").choices().getFirst();
        State state = State.empty().put(npc).remember(first, npc, choice).remember(second, npc, choice);
        state = state.reset(first, npc); assertEquals("start", state.recalledRoot(first, npc)); assertEquals("ramah", state.recalledRoot(second, npc));
    }
    @Test void changedChoiceOverwritesSameKeyWithoutDestroyingOtherKeys() {
        Definition npc = npc(); UUID player = UUID.randomUUID();
        State state = State.empty().put(npc).remember(player, npc, new Choice("Secret", "END", "rahasia", "tahu"))
                .remember(player, npc, npc.nodes().get("start").choices().getFirst()).remember(player, npc, npc.nodes().get("start").choices().get(1));
        assertEquals(Map.of("sikap", "menolak", "rahasia", "tahu"), state.memory(player, npc)); assertEquals("dingin", state.recalledRoot(player, npc));
    }
    @Test void invalidNewFieldsNeverMigrateOrEraseOldFile() throws Exception {
        var json = NpcStore.JSON.toJsonTree(State.empty().put(npc())).getAsJsonObject();
        json.getAsJsonObject("npcs").getAsJsonObject("guard").getAsJsonObject("appearance").addProperty("track", "yes");
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(json)); byte[] before = Files.readAllBytes(file);
        assertThrows(java.io.IOException.class, () -> new NpcStore(file).initialize()); assertArrayEquals(before, Files.readAllBytes(file));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
    @Test void timerSurvivesRestartAndRepeatedCompletionDoesNotExtendIt() throws Exception {
        Definition npc = npc().withGate(NpcGateDataTest.gate(GateStatus.CLOSED).opened(1000));
        State state = State.empty().put(npc).finish(UUID.randomUUID(), npc, false, UUID.randomUUID(), 20_000);
        var store = new NpcStore(directory.resolve("npc.json")); store.save(state);
        var gate = store.initialize().npcs().get("guard").gate(); assertEquals(31_000, gate.timing().closeAt());
        assertFalse(gate.due(30_999)); assertTrue(gate.due(31_000)); assertTrue(gate.due(50_000));
        assertFalse(gate.withDelay(0, 0).due(Long.MAX_VALUE));
    }
    @Test void malformedTimerRulesAndIncompleteMemoriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new GateTiming(-1, 0)); assertThrows(IllegalArgumentException.class, () -> new GateTiming(3601, 0));
        assertThrows(IllegalArgumentException.class, () -> new GateTiming(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new Choice("Yes", "END", "sikap", ""));
        var json = NpcStore.JSON.toJsonTree(npc()).getAsJsonObject(); json.addProperty("enabled", true);
        json.add("memory", NpcStore.JSON.toJsonTree(List.of(new MemoryRule("sikap", "yes", "missing"))));
        assertThrows(RuntimeException.class, () -> NpcStore.JSON.fromJson(json, Definition.class));
    }
    @Test void colouredAndPlainNamesHaveNoPrefixAndNoCommandEvents() {
        assertEquals("Penjaga Gerbang", NpcText.plain("&6Penjaga &eGerbang"));
        assertEquals("Penjaga", NpcText.plain("Penjaga")); assertNull(NpcText.name("&6Penjaga").clickEvent());
    }
    @Test void trackingKeepsSavedIdlePoseAndUsesShortestRotation() {
        Definition npc = npc(); Point idle = npc.facingLocation(), current = idle;
        for (int i = 0; i < 10; i++) current = NpcFacing.towards(npc, new Point(idle.world(), idle.x() + 4, idle.y() + npc.eyeHeight(), idle.z(), 0, 0), current);
        assertEquals(-90, current.yaw(), .001); assertEquals(0, current.pitch(), .001); assertEquals(idle, npc.facingLocation());
        assertEquals(idle.x(), current.x()); assertEquals(idle.y(), current.y()); assertEquals(idle.z(), current.z());
    }
}
