package com.bangzachery.vitae.vitaemanager.items;

/** Component ownership, independent of Bukkit's server-bound ItemMeta implementation. */
public final class StackPolicy {
    private StackPolicy() { }
    public record Decision(int baseline, int maximum, boolean restore) { }
    public static Decision decide(int current, Integer applied, Integer original,
                                  Integer requested, boolean damageable) {
        if (current < 0 || current > 99 || (requested != null && (requested < 1 || requested > 99))) {
            throw new IllegalArgumentException("Invalid stack component");
        }
        boolean owned = applied != null && original != null && applied >= 1 && applied <= 99
                && original >= 0 && original <= 99 && current == applied;
        int baseline = owned ? original : current;
        if (requested == null) return new Decision(baseline, baseline, owned);
        int maximum = baseline == 0 ? requested : Math.min(requested, baseline);
        return new Decision(baseline, damageable ? 1 : maximum, false);
    }
}