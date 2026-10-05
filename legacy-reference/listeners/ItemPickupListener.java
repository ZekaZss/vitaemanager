package com.bangzachery.vitae.vitaemanager.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public class ItemPickupListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        // Hanya berlaku untuk pemain, mob (seperti zombie/fox) tetap bisa mengambil barang secara default
        if (event.getEntity() instanceof Player player) {
            // Jika pemain tidak sedang jongkok (sneak), batalkan pengambilan barang
            if (!player.isSneaking()) {
                event.setCancelled(true);
            }
        }
    }
}