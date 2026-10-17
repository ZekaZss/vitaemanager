package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/** Slot 0 hasil terkunci; 1–9 matrix crafting, slot 5 senjata. */
public final class EvolutionGrid {
    private static final int[] SLOTS = {1, 2, 3, 4, 6, 7, 8, 9};
    private EvolutionGrid() { }
    public static boolean ingredientSlot(int rawSlot) {
        for (int slot : SLOTS) if (slot == rawSlot) return true;
        return false;
    }
    public static int rawSlot(EvolutionAltar altar, int ingredient) {
        return altar.ingredients().get(ingredient).slot();
    }
    public static int index(EvolutionAltar altar, int rawSlot) {
        if (rawSlot == 5) return 8;
        for (int i = 0; i < altar.ingredients().size(); i++)
            if (altar.ingredients().get(i).slot() == rawSlot) return i;
        return -1;
    }
    public static int rawSlot(int ingredient) {
        if (ingredient < 0 || ingredient > 7) throw new IllegalArgumentException("Nomor bahan tidak valid.");
        return SLOTS[ingredient];
    }
    public static int index(int rawSlot) {
        if (rawSlot == 5) return 8;
        for (int i = 0; i < SLOTS.length; i++) if (SLOTS[i] == rawSlot) return i;
        return -1;
    }
    public static boolean materialsComplete(EvolutionAltar altar, Map<Integer, Integer> amounts) {
        if (altar.ingredients().isEmpty()) return false;
        for (int i = 0; i < altar.ingredients().size(); i++)
            if (amounts.getOrDefault(i, 0) != altar.ingredients().get(i).count()) return false;
        return true;
    }
    public static List<Integer> newlyComplete(EvolutionAltar altar, Map<Integer, Integer> before, Map<Integer, Integer> after) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < altar.ingredients().size(); i++) {
            int required = altar.ingredients().get(i).count();
            if (before.getOrDefault(i, 0) < required && after.getOrDefault(i, 0) == required) result.add(i);
        }
        return List.copyOf(result);
    }
    public static int accepted(int required, int stored, int offered, boolean single) {
        EvolutionAltar.require(required >= 1 && stored >= 0 && stored <= required && offered >= 0,
                "Jumlah setoran tidak valid.");
        return Math.min(required - stored, Math.min(offered, single ? 1 : offered));
    }
}
