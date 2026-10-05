package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.managers.FloatingTextManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class RoleplayListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FloatingTextManager.removeText(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        FloatingTextManager.removeText(event.getEntity());
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        // Saat player teleport, entitas penumpang bakal otomatis turun, jadi mending teksnya kita bersihin aja
        FloatingTextManager.removeText(event.getPlayer());
    }
}