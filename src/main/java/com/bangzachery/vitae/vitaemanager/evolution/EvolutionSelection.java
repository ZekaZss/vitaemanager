package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.*;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.require;

/**
 * Menyimpan jumlah yang dipilih.
 * Inventory asli tetap ditangani EvolutionService.
 */
public final class EvolutionSelection {
    private final List<Integer> required;
    private final List<Map<Integer, Integer>> choices = new ArrayList<>();
    private int source = -1;

    public EvolutionSelection(List<Integer> required) {
        this.required = List.copyOf(required);
        require(!required.isEmpty() && required.size() <= 8
                        && required.stream().allMatch(n -> n >= 1 && n <= 64),
                "Jumlah bahan tidak valid.");
        required.forEach(n -> choices.add(new HashMap<>()));
    }

    public int selected(int index) {
        return choices.get(index).values().stream()
                .mapToInt(Integer::intValue).sum();
    }

    public int needed(int index) {
        return required.get(index) - selected(index);
    }

    public boolean materialsComplete() {
        for (int i = 0; i < required.size(); i++) {
            if (needed(i) != 0) return false;
        }
        return true;
    }

    public boolean complete() {
        return materialsComplete() && source >= 0;
    }

    public int source() {
        return source;
    }

    public int reserve(int ingredient, int slot, int liveAmount) {
        slot(slot);
        require(source < 0, "Reset senjata sebelum mengubah bahan.");

        int amount = Math.min(
                needed(ingredient),
                liveAmount - totals().getOrDefault(slot, 0)
        );
        require(amount > 0,
                "Bahan lengkap atau seluruh stack sudah dipilih.");

        choices.get(ingredient).merge(slot, amount, Integer::sum);
        return amount;
    }

    public void source(int slot, int liveAmount) {
        slot(slot);
        require(materialsComplete() && source < 0,
                "Lengkapi bahan dahulu atau reset senjata lama.");
        require(liveAmount > totals().getOrDefault(slot, 0),
                "Seluruh stack sudah dipilih.");
        source = slot;
    }

    public void reset(int ingredient) {
        choices.get(ingredient).clear();
        source = -1;
    }

    public void resetSource() {
        source = -1;
    }

    public Map<Integer, Integer> totals() {
        Map<Integer, Integer> result = new HashMap<>();
        choices.forEach(choice -> choice.forEach(
                (slot, n) -> result.merge(slot, n, Integer::sum)));
        if (source >= 0) result.merge(source, 1, Integer::sum);
        return Map.copyOf(result);
    }

    private static void slot(int slot) {
        require(slot >= 0 && slot < 36,
                "Gunakan slot storage inventory.");
    }
}