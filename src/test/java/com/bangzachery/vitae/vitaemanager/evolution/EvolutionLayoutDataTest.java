package com.bangzachery.vitae.vitaemanager.evolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;
import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionStore.*;

class EvolutionLayoutDataTest {
    @TempDir Path directory;
    private EvolutionAltar altar(List<Ingredient> ingredients, boolean enabled) {
        var a = EvolutionStoreTest.altar();
        return new EvolutionAltar(a.id(), a.table(), a.source(), a.result(), ingredients, a.targets(), a.preset(),
                a.ticks(), a.scale(), a.density(), a.soundRadius(), a.damageRadius(), a.damage(), enabled, a.cues(), a.music(), a.effects(), a.lethal());
    }
    @Test void sparseLayoutPreservesChosenCellsAndEscrowIndexesAcrossRestart() throws Exception {
        var a = altar(List.of(new Ingredient("AQ==", 4, new Point(EvolutionStoreTest.WORLD, 16, 64, 0), 9),
                new Ingredient("Ag==", 2, new Point(EvolutionStoreTest.WORLD, -16, 64, 0), 1)), true);
        assertEquals(0, EvolutionGrid.index(a, 9)); assertEquals(1, EvolutionGrid.index(a, 1));
        assertEquals(-1, EvolutionGrid.index(a, 2)); assertEquals(8, EvolutionGrid.index(a, 5));
        assertEquals(9, EvolutionGrid.rawSlot(a, 0));
        Deposit deposit = new Deposit(UUID.randomUUID(), EvolutionStoreTest.OWNER, a.id(), a.table(), Map.of(0, "AQ=="), null);
        State state = new State(Map.of(a.id(), a), Map.of()).deposit(deposit);
        var store = new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds());
        store.save(state); assertEquals(state, store.load());
    }
    @Test void legacyIngredientsWithoutSlotsRetainTheirOriginalPositions() {
        var store = new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds());
        var old = EvolutionStoreTest.initial(); var yaml = store.document(old); yaml.set("altars.api.ingredients.1.slot", null);
        assertEquals(old, store.parse(yaml));
        assertEquals(1, store.parse(yaml).altars().get("api").ingredients().getFirst().slot());
    }
    @Test void draftCanSaveUnmappedPedestalsButCannotBeEnabled() throws Exception {
        var material = new Ingredient("AQ==", 4, null, 7); var a = altar(List.of(material), false);
        assertFalse(a.complete());
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(material), true));
        var store = new EvolutionStore(directory.resolve("evolution.yml"), EvolutionStoreTest.sounds());
        var state = new State(Map.of(a.id(), a), Map.of()); store.save(state); assertEquals(state, store.load());
    }
    @Test void slotAndPedestalValidationRejectsDuplicatesCenterAndDistancesBeyondSixteen() {
        var point = new Point(EvolutionStoreTest.WORLD, 2, 64, 0);
        assertThrows(IllegalArgumentException.class, () -> new Ingredient("AQ==", 1, point, 5));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, point, 1), new Ingredient("Ag==", 1, new Point(point.world(), 3, 64, 0), 1)), false));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, point, 1), new Ingredient("Ag==", 1, point, 9)), false));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, new Point(point.world(), 17, 64, 0), 1)), false));
        assertThrows(IllegalArgumentException.class, () -> altar(List.of(new Ingredient("AQ==", 1, new Point(UUID.randomUUID(), 2, 64, 0), 1)), false));
    }
    @Test void lightningOnlyFiresOnFirstMaterialCompletionAndNeverOnWeaponOrRepeatedRender() {
        var a = EvolutionStoreTest.altar();
        assertEquals(List.of(), EvolutionGrid.newlyComplete(a, Map.of(), Map.of(0, 2)));
        assertEquals(List.of(0), EvolutionGrid.newlyComplete(a, Map.of(0, 2), Map.of(0, 4)));
        assertEquals(List.of(), EvolutionGrid.newlyComplete(a, Map.of(0, 4), Map.of(0, 4, 8, 1)));
        assertEquals(List.of(), EvolutionGrid.newlyComplete(a, Map.of(0, 4), Map.of(0, 4)));
    }
}
