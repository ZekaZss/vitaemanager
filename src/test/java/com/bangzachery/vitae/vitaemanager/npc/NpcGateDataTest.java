package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;

class NpcGateDataTest {
    @TempDir Path directory;
    static final UUID WORLD = UUID.fromString("11111111-1111-1111-1111-111111111111");
    static Definition npc(String id) { return Definition.create(id, "Penjaga", new Point(WORLD, 4, 64, 0, 0, 0), new Point(WORLD, 3, 64, 0, 0, 0)); }
    static Gate gate(GateStatus status) { return new Gate(new WhisperArea(WORLD, 0, 64, 0, 0, 64, 0), List.of(new GateBlock(0, 64, 0, "minecraft:stone")), status); }
    @Test void oldNpcJsonWithoutFacingOrGateRetainsProgressQuotasAndPendingReward() throws Exception {
        var json = NpcStore.JSON.toJsonTree(npc("guard")).getAsJsonObject();
        json.add("gift", NpcStore.JSON.toJsonTree(new Reward(new BigDecimal("1000"), List.of())));
        var definition = NpcStore.JSON.fromJson(json, Definition.class); UUID owner = UUID.randomUUID();
        State state = State.empty().put(definition).finish(owner, definition, false, UUID.randomUUID(), 100);
        var root = NpcStore.JSON.toJsonTree(state).getAsJsonObject(); var record = root.getAsJsonObject("npcs").getAsJsonObject("guard");
        record.remove("facing"); record.remove("gate");
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(root));
        var store = new NpcStore(file); assertEquals(state, store.initialize()); store.save(store.initialize()); assertEquals(state, store.initialize());
    }
    @Test void allGateStatusesSurviveRestartAndDoNotRemoveQuestHistory() throws Exception {
        var store = new NpcStore(directory.resolve("npc.json")); UUID player = UUID.randomUUID();
        for (GateStatus status : GateStatus.values()) {
            Definition definition = npc("guard").withGate(gate(status));
            State state = State.empty().put(definition).heard(player, definition); store.save(state);
            assertEquals(state, store.initialize()); assertTrue(store.initialize().done(player, definition));
        }
    }
    @Test void openChoiceNeedsConfiguredGateAndCountsAsAnExit() {
        var json = NpcStore.JSON.toJsonTree(npc("guard")).getAsJsonObject(); json.addProperty("enabled", true);
        json.add("nodes", NpcStore.JSON.toJsonTree(Map.of("start", new Node("Masuklah.", List.of(new Choice("Buka", "OPEN"))))));
        assertThrows(RuntimeException.class, () -> NpcStore.JSON.fromJson(json, Definition.class));
        json.add("gate", NpcStore.JSON.toJsonTree(gate(GateStatus.CLOSED)));
        assertNotNull(NpcStore.JSON.fromJson(json, Definition.class));
    }
    @Test void gateSelectionRejectsLargeSnapshotsDuplicatesAndOutsideCoordinates() {
        var block = new GateBlock(0, 64, 0, "minecraft:stone"); var area = gate(GateStatus.CLOSED).area();
        assertThrows(IllegalArgumentException.class, () -> new Gate(area, List.of(block, block), GateStatus.CLOSED));
        assertThrows(IllegalArgumentException.class, () -> new Gate(area, List.of(new GateBlock(1, 64, 0, "minecraft:stone")), GateStatus.CLOSED));
        assertDoesNotThrow(() -> new Gate(new WhisperArea(WORLD, 0, 64, 0, 16, 80, 0), List.of(block), GateStatus.CLOSED));
    }
    @Test void distinctNpcsCannotClaimTheSameWall() {
        var first = npc("first").withGate(gate(GateStatus.CLOSED)); var second = npc("second").withGate(gate(GateStatus.CLOSED));
        assertThrows(IllegalArgumentException.class, () -> State.empty().put(first).put(second));
    }
    @Test void finishByAnotherPlayerCannotOverwriteOpeningStatusAndRewardsRemainOnceOnly() {
        var json = NpcStore.JSON.toJsonTree(npc("guard").withGate(gate(GateStatus.CLOSED))).getAsJsonObject();
        json.add("gift", NpcStore.JSON.toJsonTree(new Reward(new BigDecimal("1000"), List.of())));
        var closed = NpcStore.JSON.fromJson(json, Definition.class); UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        State state = State.empty().put(closed).finish(first, closed, false, UUID.randomUUID(), 100).put(closed.withGate(gate(GateStatus.OPENING)));
        state = state.finish(second, closed, false, UUID.randomUUID(), 101);
        assertEquals(GateStatus.OPENING, state.npcs().get("guard").gate().status()); assertEquals(2, state.pending().size());
        UUID receipt = state.pending().get(first).receipt(); state = state.acknowledge(first, receipt);
        assertSame(state, state.finish(first, closed, false, UUID.randomUUID(), 102));
        assertEquals(0, state.quota(first, 103).count());
    }
    @Test void invalidGateDataIsRejectedWithoutErasingSavedNpcFile() throws Exception {
        var store = new NpcStore(directory.resolve("npc.json")); State state = State.empty().put(npc("guard").withGate(gate(GateStatus.CLOSED))); store.save(state);
        var root = NpcStore.JSON.toJsonTree(state).getAsJsonObject(); root.getAsJsonObject("npcs").getAsJsonObject("guard").getAsJsonObject("gate").addProperty("status", true);
        Path file = directory.resolve("npc.json"); Files.writeString(file, NpcStore.JSON.toJson(root)); byte[] before = Files.readAllBytes(file);
        assertThrows(java.io.IOException.class, store::initialize); assertArrayEquals(before, Files.readAllBytes(file));
    }
}
