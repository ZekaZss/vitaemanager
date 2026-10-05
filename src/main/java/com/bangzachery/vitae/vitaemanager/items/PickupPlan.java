package com.bangzachery.vitae.vitaemanager.items;

import java.util.ArrayList;
import java.util.List;

/** Pure allocation; adapters retain exact original ItemStack identities and metadata. */
public record PickupPlan(List<Integer> added, int remaining) {
    public PickupPlan { added = List.copyOf(added); }
    public record Slot(int amount, boolean similar) {
        public Slot { if (amount < 0) throw new IllegalArgumentException("Negative slot count"); }
    }

    public static PickupPlan allocate(int amount, int limit, List<Slot> slots) {
        if (amount < 0 || limit < 1 || limit > 99) throw new IllegalArgumentException("Invalid pickup amount/limit");
        List<Integer> additions = new ArrayList<>();
        for (int index = 0; index < slots.size(); index++) additions.add(0);
        int remaining = amount;
        for (int pass = 0; pass < 2; pass++) {
            for (int index = 0; index < slots.size() && remaining > 0; index++) {
                Slot slot = slots.get(index);
                if (pass == 0 ? slot.amount() == 0 || !slot.similar() : slot.amount() != 0) continue;
                int moved = Math.min(remaining, Math.max(0, limit - slot.amount()));
                additions.set(index, moved);
                remaining -= moved;
            }
        }
        return new PickupPlan(additions, remaining);
    }

    /** Extra empty storage slots needed to split a stack without deleting overflow. */
    public static long extraSlots(int amount, int limit) {
        if (amount < 1 || limit < 1 || limit > 99) throw new IllegalArgumentException("Invalid split amount/limit");
        return (amount - 1L) / limit;
    }
}