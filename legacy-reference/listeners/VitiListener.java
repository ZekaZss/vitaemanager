package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.commands.VitiCommand;
import com.bangzachery.vitae.vitaemanager.managers.PDCManager;
import com.bangzachery.vitae.vitaemanager.managers.VitiFileManager;
import com.bangzachery.vitae.vitaemanager.utils.Keys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class VitiListener implements Listener {

    private final MiniMessage mm = MiniMessage.miniMessage();

    // SINKRONISASI SAAT LOGIN
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        VitiFileManager.syncFileToPDC(event.getPlayer());
    }

    // Mencairkan kertas fisik menjadi saldo digital
    @EventHandler
    public void onPaperInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.PAPER) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Double amount = meta.getPersistentDataContainer().get(Keys.VITI_PAPER, PersistentDataType.DOUBLE);
        if (amount != null) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            PDCManager.addViti(player, amount);
            item.setAmount(item.getAmount() - 1);

            player.sendMessage(mm.deserialize("<green>Kamu mencairkan Uang Viti Fisik sebesar <white>" + VitiCommand.formatViti(amount)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
        }
    }

    // Blokir uang kertas di Anvil
    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        ItemStack item1 = event.getInventory().getItem(0);
        ItemStack item2 = event.getInventory().getItem(1);

        if (isVitiPaper(item1) || isVitiPaper(item2)) {
            event.setResult(null);
        }
    }

    private boolean isVitiPaper(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Keys.VITI_PAPER, PersistentDataType.DOUBLE);
    }
}