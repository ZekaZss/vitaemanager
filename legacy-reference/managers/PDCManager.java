package com.bangzachery.vitae.vitaemanager.managers;

import com.bangzachery.vitae.vitaemanager.utils.Keys;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public final class PDCManager {

    public static double getViti(Player player) {
        return player.getPersistentDataContainer().getOrDefault(Keys.VITI, PersistentDataType.DOUBLE, 0.0);
    }

    public static void setViti(Player player, double amount) {
        // 1. Simpan ke Memori RAM (PDC)
        player.getPersistentDataContainer().set(Keys.VITI, PersistentDataType.DOUBLE, amount);
        // 2. Tembuskan perubahan tersebut ke dalam viti.yml
        VitiFileManager.updatePlayer(player, amount);
    }

    public static void addViti(Player player, double amount) {
        setViti(player, getViti(player) + amount);
    }

    public static void removeViti(Player player, double amount) {
        double bal = getViti(player) - amount;
        if (bal < 0) bal = 0;
        setViti(player, bal);
    }

    // -- Fitur Essentials --
    public static boolean isVanished(Player player) {
        return player.getPersistentDataContainer().getOrDefault(Keys.VANISHED, PersistentDataType.BOOLEAN, false);
    }
    public static void setVanished(Player player, boolean state) {
        if (state) player.getPersistentDataContainer().set(Keys.VANISHED, PersistentDataType.BOOLEAN, true);
        else player.getPersistentDataContainer().remove(Keys.VANISHED);
    }
    public static boolean isFrozen(Player player) {
        return player.getPersistentDataContainer().getOrDefault(Keys.FROZEN, PersistentDataType.BOOLEAN, false);
    }
    public static void setFrozen(Player player, boolean state) {
        if (state) player.getPersistentDataContainer().set(Keys.FROZEN, PersistentDataType.BOOLEAN, true);
        else player.getPersistentDataContainer().remove(Keys.FROZEN);
    }
}