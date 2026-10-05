package com.bangzachery.vitae.vitaemanager.listeners;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.Plugin;

public class EssentialsIntegrator implements Listener {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Plugin plugin;

    public EssentialsIntegrator(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().toLowerCase().split(" ")[0];

        // 1. Memblokir Command Ekonomi EssentialsX
        if (command.equals("/money") || command.equals("/bal") || command.equals("/balance")
                || command.equals("/pay") || command.equals("/eco")) {

            event.setCancelled(true);
            event.getPlayer().sendMessage(mm.deserialize("<red>Command ini telah diblokir! Gunakan sistem uang roleplay: <yellow>/viti"));
            return;
        }

        // 2. Memastikan Admin yang memakai /vanish benar-benar bersih dari pantauan
        if (command.equals("/vanish") || command.equals("/v") || command.equals("/evanish")) {
            Player player = event.getPlayer();
            // Beri jeda 2 tick agar EssentialsX selesai mengatur metadata vanish-nya
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.hasMetadata("vanished") && player.getMetadata("vanished").get(0).asBoolean()) {
                    player.sendMessage(mm.deserialize("<green>Kamu kini tersembunyi dari GUI Player dan Tablist."));
                }
            }, 2L);
        }
    }
}