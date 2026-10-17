package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcData.*;
import static org.junit.jupiter.api.Assertions.*;

class NpcFarmDataTest {
    @TempDir Path directory;
    static final UUID WORLD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    static final WhisperArea AREA = WhisperArea.between(WORLD, -6323, 67, -4180, -6364, 72, -4235);
    static final Point START = new Point(WORLD, -6340.5, 68, -4210.5, 0, 0);
    static List<Crop> crops(int count) {
        List<Crop> crops = new ArrayList<>();
        for (int i = 0; i < count; i++) crops.add(new Crop(-6364 + i % 42, 68, -4235 + i / 42,
                "minecraft:carrots[age=7]", "minecraft:farmland[moisture=7]"));
        return crops;
    }
    static Quest farm(int count) { return new Quest(QuestKind.FARM, AREA, START, 5, "COW", crops(count), new Reward(new BigDecimal("10000"), List.of())); }
    static Definition npc(Quest quest) {
        var json = NpcStore.JSON.toJsonTree(Definition.create("petani_revenant", "Petani Revenant", START, START)).getAsJsonObject();
        json.add("quest", NpcStore.JSON.toJsonTree(quest));
        return NpcStore.JSON.fromJson(json, Definition.class);
    }
    @Test void fullRevenantFieldExceedsOldLimitsAndKeepsAllPlants() {
        Quest quest = farm(2352);
        assertEquals(2352, quest.crops().size()); assertEquals(5, quest.goal()); assertEquals(0, new BigDecimal("10000").compareTo(quest.reward().viti()));
        assertEquals(14112, new NpcFarmCursor(AREA).volume());
    }
    @Test void largeFarmSurvivesRestartWithRewardIdentityMemoriesAndGlobalQuota() throws Exception {
        Definition npc = npc(farm(2352)); UUID player = UUID.randomUUID(), receipt = UUID.randomUUID();
        State state = State.empty().put(npc).remember(player, npc, new Choice("Ya", "QUEST", "ladang", "revenant"))
                .finish(player, npc, true, receipt, 1000);
        NpcStore store = new NpcStore(directory.resolve("npc.json")); store.save(state);
        State reloaded = store.initialize();
        assertEquals(state, reloaded); assertEquals(npc.token(), reloaded.npcs().get(npc.id()).token());
        assertEquals(1, reloaded.quota(player, 1001).count()); assertEquals(receipt, reloaded.pending().get(player).receipt());
        assertEquals("revenant", reloaded.memory(player, npc).get("ladang"));
    }
    @Test void threeCompletedLargeQuestsStillStartTwentyMinuteCooldown() {
        Definition npc = npc(farm(600)); UUID player = UUID.randomUUID(); State state = State.empty().put(npc);
        for (int i = 0; i < 3; i++) {
            UUID receipt = UUID.randomUUID(); state = state.finish(player, npc, true, receipt, 1000);
            assertEquals(0, new BigDecimal("10000").compareTo(state.pending().get(player).reward().viti()));
            state = state.acknowledge(player, receipt);
        }
        assertEquals(3, state.quota(player, 1001).count()); assertEquals(0, state.quota(player, 1_201_000).count());
    }
    @Test void emptyDuplicateOutsideAndItemRewardFarmDefinitionsRemainInvalid() {
        assertThrows(IllegalArgumentException.class, () -> farm(0)); Crop first = crops(1).getFirst();
        assertThrows(IllegalArgumentException.class, () -> new Quest(QuestKind.FARM, AREA, START, 5, "COW", List.of(first, first), Reward.none()));
        assertThrows(IllegalArgumentException.class, () -> new Quest(QuestKind.FARM, AREA, START, 5, "COW",
                List.of(new Crop(0, 68, 0, first.plant(), first.soil())), Reward.none()));
        assertThrows(IllegalArgumentException.class, () -> new Quest(QuestKind.FARM, AREA, START, 5, "COW", List.of(first), new Reward(BigDecimal.ZERO, List.of("item"))));
    }
    @Test void cursorVisitsAllNegativeCoordinatesOnceIncludingBothCorners() {
        NpcFarmCursor cursor = new NpcFarmCursor(AREA); Set<String> positions = new HashSet<>(); Set<String> finishedChunks = new HashSet<>(); String chunk = "";
        while (!cursor.done()) {
            String current = cursor.chunkX() + ":" + cursor.chunkZ();
            if (!chunk.equals(current)) { if (!chunk.isEmpty()) finishedChunks.add(chunk); assertFalse(finishedChunks.contains(current)); chunk = current; }
            assertTrue(positions.add(cursor.x() + ":" + cursor.y() + ":" + cursor.z())); cursor.next();
        }
        assertEquals(14112, positions.size()); assertEquals(14112, cursor.visited());
        assertTrue(positions.contains("-6323:67:-4180")); assertTrue(positions.contains("-6364:72:-4235"));
    }
    @Test void singleBlockCursorAndChunkBoundaryFinishWithoutExtraCells() {
        for (int x : List.of(-17, -16, -1, 0, 15, 16)) {
            NpcFarmCursor cursor = new NpcFarmCursor(new WhisperArea(WORLD, x, 68, x, x, 68, x));
            assertEquals(x >> 4, cursor.chunkX()); assertEquals(1, cursor.volume()); cursor.next(); cursor.next();
            assertTrue(cursor.done()); assertEquals(1, cursor.visited());
        }
    }
    @Test void legacySmallFarmDoesNotNeedSchemaChangeOrLoseData() throws Exception {
        State state = State.empty().put(npc(farm(10))); Path file = directory.resolve("npc.json");
        Files.writeString(file, NpcStore.JSON.toJson(state)); byte[] old = Files.readAllBytes(file);
        assertEquals(state, new NpcStore(file).initialize()); assertArrayEquals(old, Files.readAllBytes(file));
    }
    @Test void validNpcFileAboveSixteenMegabytesStillLoads() throws Exception {
        State state = State.empty().put(npc(farm(600))); Path file = directory.resolve("npc.json");
        Files.writeString(file, NpcStore.JSON.toJson(state));
        try (var writer = Files.newBufferedWriter(file, StandardOpenOption.APPEND)) {
            String whitespace = " ".repeat(65536); for (int i = 0; i < 250; i++) writer.write(whitespace);
        }
        assertTrue(Files.size(file) > 16_000_000); assertEquals(state, new NpcStore(file).initialize());
    }
    @Test void farmSnapshotLargerThanOldFileCapCanBeSavedAndReopened() throws Exception {
        WhisperArea area = new WhisperArea(WORLD, 0, 64, 0, 255, 68, 255); List<Crop> crops = new ArrayList<>();
        for (int i = 0; i < 90_000; i++) crops.add(new Crop(i % 256, 65 + 2 * (i / 65536), i / 256 % 256,
                "minecraft:wheat[age=7]", "minecraft:farmland[moisture=7]"));
        Quest quest = new Quest(QuestKind.FARM, area, new Point(WORLD, .5, 65, .5, 0, 0), 5, "COW", crops, Reward.none());
        State state = State.empty().put(npc(quest)); NpcStore store = new NpcStore(directory.resolve("npc.json")); store.save(state);
        assertTrue(Files.size(directory.resolve("npc.json")) > 16_000_000); assertEquals(state, store.initialize());
    }
}
