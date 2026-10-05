package com.bangzachery.vitae.vitaemanager.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;

public class StuckItemListener implements Listener {

    // Cache agar server tidak membaca config.yml terus menerus
    public static final Map<Material, Integer> stuckLimits = new HashMap<>();

    public StuckItemListener(Plugin plugin) {
        stuckLimits.clear();
        if (plugin.getConfig().contains("stuck_items")) {
            for (String key : plugin.getConfig().getConfigurationSection("stuck_items").getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    int limit = plugin.getConfig().getInt("stuck_items." + key);
                    stuckLimits.put(mat, limit);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        applyStackLimit(event.getItem().getItemStack());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null) applyStackLimit(event.getCurrentItem());
        if (event.getCursor() != null) applyStackLimit(event.getCursor());
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        for (ItemStack item : event.getInventory().getContents()) {
            applyStackLimit(item);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (ItemStack item : event.getPlayer().getInventory().getContents()) {
            applyStackLimit(item);
        }
    }

    // Fungsi utama penyuntikan batas (Vanilla 1.21 Component)
    private void applyStackLimit(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        Material mat = item.getType();
        if (stuckLimits.containsKey(mat)) {
            int limit = stuckLimits.get(mat);
            ItemMeta meta = item.getItemMeta();

            // Jika item belum punya batas yang sesuai, timpa menggunakan fitur native
            if (meta != null && (!meta.hasMaxStackSize() || meta.getMaxStackSize() != limit)) {
                meta.setMaxStackSize(limit);
                item.setItemMeta(meta);
            }
        }
    }
}