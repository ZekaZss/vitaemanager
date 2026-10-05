package com.bangzachery.vitae.vitaemanager.totem;

/** Existing PDC count and epoch-millisecond cooldown, independent of Bukkit. */
public record TotemState(int count, long cooldownUntil) {
    public TotemState {
        if (count < 0 || cooldownUntil < 0) throw new IllegalArgumentException("Negative totem data");
    }

    public TotemState absorb(int limit, long now) {
        requireLimit(limit);
        requireTime(now);
        if (now < cooldownUntil) throw new Failure("totem-cooldown");
        if (count >= limit) throw new Failure("totem-full");
        return new TotemState(Math.addExact(count, 1), cooldownUntil);
    }

    public TotemState use(long now, long cooldownMillis) {
        requireTime(now);
        if (cooldownMillis < 0) throw new IllegalArgumentException("Negative cooldown duration");
        if (count == 0) throw new Failure("totem-empty");
        // Active absorption cooldown is not extended by spending another stored charge.
        long deadline = now < cooldownUntil ? cooldownUntil : Math.addExact(now, cooldownMillis);
        return new TotemState(count - 1, deadline);
    }

    public TotemState resetCooldown() { return new TotemState(count, 0); }

    public long remainingSeconds(long now) {
        requireTime(now);
        if (now >= cooldownUntil) return 0;
        long remaining = cooldownUntil - now;
        return remaining / 1000 + (remaining % 1000 == 0 ? 0 : 1);
    }

    public static boolean fatal(double health, double finalDamage) {
        return Double.isFinite(health) && health > 0 && Double.isFinite(finalDamage)
                && finalDamage >= health;
    }

    public static void requireLimit(int limit) {
        if (limit < 1 || limit > 1000) throw new IllegalArgumentException("Totem limit must be 1..1000");
    }

    private static void requireTime(long now) {
        if (now < 0) throw new IllegalArgumentException("Negative timestamp");
    }

    public static final class Failure extends RuntimeException {
        private final String key;
        public Failure(String key) { super(key); this.key = key; }
        public String key() { return key; }
    }
}