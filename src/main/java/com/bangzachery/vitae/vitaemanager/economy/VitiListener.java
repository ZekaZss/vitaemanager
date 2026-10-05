package com.bangzachery.vitae.vitaemanager.economy;

import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class VitiListener implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final VitiService service;
    private final VitiPaper paper;
    private final MessageService messages;
    private BukkitTask task;

    public VitiListener(JavaPlugin plugin, VitiService service, VitiPaper paper, MessageService messages) {
        this.plugin = plugin; this.service = service; this.paper = paper; this.messages = messages;
    }

    public void start() {
        service.tick();
        task = Bukkit.getScheduler().runTaskTimer(plugin, service::tick, 20L, 20L);
    }

    @EventHandler public void join(PlayerJoinEvent event) { service.tick(); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !paper.isPaper(event.getItem())) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.useItemInHand() == org.bukkit.event.Event.Result.DENY
                || (event.getAction() == Action.RIGHT_CLICK_BLOCK
                && event.useInteractedBlock() == org.bukkit.event.Event.Result.DENY)) return;
        // Air interactions can arrive cancelled by vanilla prediction; do not skip them.
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("vitae.viti")) { messages.send(event.getPlayer(), "no-permission"); return; }
        service.redeem(event.getPlayer(), key -> {
            if (event.getPlayer().isOnline()) messages.send(event.getPlayer(), key);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void anvil(PrepareAnvilEvent event) {
        if (paper.isPaper(event.getInventory().getItem(0)) || paper.isPaper(event.getInventory().getItem(1))) {
            event.setResult(null);
        }
    }

    @Override public void close() { if (task != null) task.cancel(); }
}