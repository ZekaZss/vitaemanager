package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.managers.BlockerFileManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockerListener implements Listener {
    public static final Set<Material> blockedItems = new HashSet<>();
    public static final Set<EntityType> blockedMobs = new HashSet<>();
    private final MiniMessage mm = MiniMessage.miniMessage();

    public BlockerListener() {
        loadBlocks();
    }

    public static void loadBlocks() {
        blockedItems.clear();
        blockedMobs.clear();

        // Mengambil data dari blocked.yml, bukan config.yml utama
        List<String> items = BlockerFileManager.getConfig().getStringList("blocked_items");
        for (String i : items) {
            try { blockedItems.add(Material.valueOf(i)); } catch (IllegalArgumentException ignored) {}
        }

        List<String> mobs = BlockerFileManager.getConfig().getStringList("blocked_mobs");
        for (String m : mobs) {
            try { blockedMobs.add(EntityType.valueOf(m)); } catch (IllegalArgumentException ignored) {}
        }
    }

    private void destroyItem(Player player, ItemStack item) {
        if (item != null && blockedItems.contains(item.getType())) {
            item.setAmount(0);
            player.sendActionBar(mm.deserialize("<red><bold>Benda terlarang dihancurkan dari tangan Anda!</bold></red>"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickupBlocked(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (blockedItems.contains(event.getItem().getItemStack().getType())) {
                event.setCancelled(true);
                event.getItem().remove();
                player.sendActionBar(mm.deserialize("<red>Item tersebut diblokir oleh server.</red>"));
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && blockedItems.contains(event.getCurrentItem().getType())) {
            event.getCurrentItem().setAmount(0);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        destroyItem(player, item);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item != null && blockedItems.contains(item.getType())) {
                item.setAmount(0);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (blockedMobs.contains(event.getEntityType())) {
            event.setCancelled(true);
        }
    }
}