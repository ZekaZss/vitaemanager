package com.bangzachery.vitae.vitaemanager.profile;

/** Counts only; the GUI retains and clones the original ItemStack metadata. */
public final class StackExchange {
    private StackExchange() { }

    public record Result(int slot, int cursor, boolean swapped) { }

    public static Result move(int slot, int cursor, boolean similar,
                              int slotLimit, int cursorLimit, int capacity, boolean right) {
        if (slot < 0 || cursor < 0 || slotLimit < 1 || cursorLimit < 1 || capacity < 1) {
            throw new IllegalArgumentException("Invalid stack counts or limits");
        }
        Result unchanged = new Result(slot, cursor, false);
        // Preserve abnormal legacy stacks rather than silently clamping/deleting items.
        if (slot > slotLimit || cursor > cursorLimit) return unchanged;
        if (cursor == 0) {
            int taken = right ? slot / 2 + slot % 2 : slot;
            return new Result(slot - taken, taken, false);
        }
        if (slot == 0 || similar) {
            int limit = Math.min(capacity, slot == 0 ? cursorLimit : slotLimit);
            int placed = Math.min(right ? 1 : cursor, Math.max(0, limit - slot));
            return new Result(slot + placed, cursor - placed, false);
        }
        if (cursor > capacity) return unchanged;
        return new Result(cursor, slot, true);
    }
}