package com.bangzachery.vitae.vitaemanager.managers;

import com.bangzachery.vitae.vitaemanager.utils.Keys;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public final class TotemManager {
    // Batas default jika admin belum menyetelnya via command
    private static int limit = 3;

    public static int getLimit() {
        return limit;
    }

    public static void setLimit(int newLimit) {
        limit = newLimit;
    }

    public static int getAbsorbed(Player player) {
        return player.getPersistentDataContainer().getOrDefault(Keys.TOTEM_COUNT, PersistentDataType.INTEGER, 0);
    }

    public static void setAbsorbed(Player player, int amount) {
        player.getPersistentDataContainer().set(Keys.TOTEM_COUNT, PersistentDataType.INTEGER, amount);
    }

    public static long getCooldown(Player player) {
        return player.getPersistentDataContainer().getOrDefault(Keys.TOTEM_COOLDOWN, PersistentDataType.LONG, 0L);
    }

    public static void setCooldown(Player player, long time) {
        if (time <= 0) {
            player.getPersistentDataContainer().remove(Keys.TOTEM_COOLDOWN);
        } else {
            player.getPersistentDataContainer().set(Keys.TOTEM_COOLDOWN, PersistentDataType.LONG, time);
        }
    }

    public static void startCooldown(Player player) {
        long fifteenHours = 15L * 60L * 60L * 1000L;
        setCooldown(player, System.currentTimeMillis() + fifteenHours);
    }
}