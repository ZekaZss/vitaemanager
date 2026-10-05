package com.bangzachery.vitae.vitaemanager.integration;

import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class VillagerTradeListener implements Listener {
    private static final Component MESSAGE = Component.text(
            "[Vitae] Trading dengan villager biasa dilarang. "
                    + "Silakan datang ke villager toko yang sudah disediakan di masing-masing base.",
            NamedTextColor.YELLOW);

    private final Supplier<Plugin> shopkeepers;
    private final LongSupplier clock;
    private final Map<UUID, Long> feedback = new HashMap<>();

    public VillagerTradeListener() {
        this(() -> Bukkit.getPluginManager().getPlugin("Shopkeepers"), System::nanoTime);
    }

    VillagerTradeListener(Supplier<Plugin> shopkeepers, LongSupplier clock) {
        this.shopkeepers = shopkeepers;
        this.clock = clock;
    }

    private boolean blocked(Entity entity) {
        if (!(entity instanceof AbstractVillager)) return false;

        Plugin shops = shopkeepers.get();
        if (shops == null || !shops.isEnabled()) return true;

        // Pengecualian hanya untuk penanda milik plugin Shopkeepers.
        return entity.getMetadata("shopkeeper").stream().noneMatch(value ->
                value.getOwningPlugin() == shops);
    }

    private void denied(Player player) {
        long now = clock.getAsLong();
        Long last = feedback.get(player.getUniqueId());

        if (last == null || now - last >= 1_000_000_000L) {
            feedback.put(player.getUniqueId(), now);
            player.sendMessage(MESSAGE);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEntityEvent event) {
        if (!blocked(event.getRightClicked())) return;

        event.setCancelled(true);
        denied(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interactAt(PlayerInteractAtEntityEvent event) {
        interact(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void open(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)
                || !(event.getInventory() instanceof MerchantInventory inventory)
                || !(inventory.getMerchant() instanceof AbstractVillager villager)
                || !blocked(villager)) return;

        event.setCancelled(true);
        denied(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void trade(PlayerTradeEvent event) {
        if (!blocked(event.getVillager())) return;

        event.setCancelled(true);
        denied(event.getPlayer());
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        feedback.remove(event.getPlayer().getUniqueId());
    }
}