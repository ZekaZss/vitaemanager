package com.bangzachery.vitae.vitaemanager.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public final class Keys {
    public static NamespacedKey VITI;
    public static NamespacedKey VITI_PAPER;
    public static NamespacedKey FROZEN;
    public static NamespacedKey VANISHED;
    public static NamespacedKey TOTEM_COUNT;
    public static NamespacedKey TOTEM_COOLDOWN;

    public static void init(Plugin plugin) {
        VITI = new NamespacedKey(plugin, "viti_balance");
        VITI_PAPER = new NamespacedKey(plugin, "viti_paper_amount");
        FROZEN = new NamespacedKey(plugin, "is_frozen");
        VANISHED = new NamespacedKey(plugin, "is_vanished");
        TOTEM_COUNT = new NamespacedKey(plugin, "totem_count");
        TOTEM_COOLDOWN = new NamespacedKey(plugin, "totem_cooldown");
    }
}