package com.bangzachery.vitae.vitaemanager.integration;

import com.bangzachery.vitae.vitaemanager.core.config.ConfigurationService;
import com.bangzachery.vitae.vitaemanager.core.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class EssentialsListener implements Listener {
    private final ConfigurationService configuration;
    private final MessageService messages;

    public EssentialsListener(ConfigurationService configuration, MessageService messages) {
        this.configuration = configuration;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void command(PlayerCommandPreprocessEvent event) {
        if (!configuration.current().items().blockEssentialsEconomy()) return;
        var essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null || !essentials.isEnabled()) return;
        String label = EssentialsPolicy.label(event.getMessage());
        var resolved = Bukkit.getPluginCommand(label);
        String owner = resolved == null ? label.startsWith("essentials:") ? "Essentials" : "" : resolved.getPlugin().getName();
        String canonical = resolved == null ? label : resolved.getName();
        if (EssentialsPolicy.blocked(label, canonical, owner)) {
            event.setCancelled(true);
            messages.send(event.getPlayer(), "essentials-economy-blocked");
        }
    }
}