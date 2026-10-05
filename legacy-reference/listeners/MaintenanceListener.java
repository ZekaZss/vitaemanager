package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.managers.ServerState;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class MaintenanceListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        // Cek langsung ke ServerState Anda
        if (ServerState.isMaintenance) {

            // Jika player BUKAN OP dan TIDAK punya permission admin, tolak login-nya
            if (!event.getPlayer().isOp() && !event.getPlayer().hasPermission("vitae.admin.maintenance")) {
                event.disallow(
                        PlayerLoginEvent.Result.KICK_OTHER,
                        mm.deserialize("<red><bold>SERVER MAINTENANCE</bold>\n\n<gray>Server sedang dalam perbaikan.\nHarap kembali lagi nanti!")
                );
            }
        }
    }
}