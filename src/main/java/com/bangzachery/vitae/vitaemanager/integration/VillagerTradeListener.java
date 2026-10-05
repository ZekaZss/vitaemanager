package com.bangzachery.vitae.vitaemanager.integration;

import io.papermc.paper.event.player.PlayerTradeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.function.*;

/** Keeps Shopkeepers entity shops and virtual merchants available. */
public final class VillagerTradeListener implements Listener {
    private final Supplier<Plugin> shops;
    private final LongSupplier clock;
    private final Map<UUID, Long> feedback = new HashMap<>();
    private static final Component MESSAGE = Component.text("[Vitae] Trading dengan villager biasa dilarang. Silakan datang ke villager toko yang sudah disediakan di masing-masing base.", NamedTextColor.YELLOW);
    public VillagerTradeListener() { this(() -> Bukkit.getPluginManager().getPlugin("Shopkeepers"), System::nanoTime); }
    VillagerTradeListener(Supplier<Plugin> shops, LongSupplier clock) { this.shops = shops; this.clock = clock; }
    private boolean blocked(Entity entity) {
        if (!(entity instanceof AbstractVillager)) return false;
        Plugin owner = shops.get();
        return owner == null || !owner.isEnabled() || entity.getMetadata("shopkeeper").stream().noneMatch(m -> m.getOwningPlugin() == owner);
    }
    private void denied(Player player) {
        long now = clock.getAsLong(); Long old = feedback.get(player.getUniqueId());
        if (old == null || now - old >= 1_000_000_000L) { feedback.put(player.getUniqueId(), now); player.sendMessage(MESSAGE); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void interact(PlayerInteractEntityEvent e) {
        if (blocked(e.getRightClicked())) { e.setCancelled(true); denied(e.getPlayer()); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void interactAt(PlayerInteractAtEntityEvent e) { interact(e); }
    @EventHandler(priority = EventPriority.HIGHEST) public void open(InventoryOpenEvent e) {
        if (e.getInventory() instanceof MerchantInventory inventory && inventory.getMerchant() instanceof AbstractVillager villager
                && blocked(villager)) { e.setCancelled(true); if (e.getPlayer() instanceof Player player) denied(player); }
    }
    @EventHandler(priority = EventPriority.HIGHEST) public void trade(PlayerTradeEvent e) {
        if (blocked(e.getVillager())) { e.setCancelled(true); denied(e.getPlayer()); }
    }
    @EventHandler public void quit(PlayerQuitEvent e) { feedback.remove(e.getPlayer().getUniqueId()); }
}
