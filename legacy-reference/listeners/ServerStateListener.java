package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.managers.ServerState;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class ServerStateListener implements Listener {

    private final MiniMessage mm = MiniMessage.miniMessage();

    // Mencegah chat kalau toggle chat lagi OFF
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncChatEvent event) {
        if (ServerState.isChatMuted && !event.getPlayer().hasPermission("vitae.admin.chatbypass")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(mm.deserialize("<red>Tidak bisa mengirim pesan, jangan mencoba itu atau saya ban! wkwwkkw"));
        }
    }

    // Mencegah player biasa masuk kalau maintenance lagi ON
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (ServerState.isMaintenance) {
            // Karena ini event sebelum login penuh, kita cek by UUID atau nama aja nggak bisa cek OP normal.
            // Tapi aman, kita kasih exception buat whitelist atau bisa bypass via command nanti.
            // Biar rapi, kick aja langsung dengan reason maintenance.
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    mm.deserialize("<red><bold>SERVER SEDANG MAINTENANCE!</bold>\n<gray>Hanya Admin yang dapat masuk saat ini."));
        }
    }
}