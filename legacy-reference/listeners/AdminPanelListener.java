package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.Vitaemanager;
import com.bangzachery.vitae.vitaemanager.guis.AdminHolders;
import com.bangzachery.vitae.vitaemanager.guis.AdminPanelGUI;
import com.bangzachery.vitae.vitaemanager.managers.ServerState;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AdminPanelListener implements Listener {
    private final MiniMessage mm = MiniMessage.miniMessage();

    @EventHandler
    public void onAdminPanelClick(InventoryClickEvent event) {
        // Cek jika ini adalah MainPanelHolder asli Anda
        if (event.getInventory().getHolder() instanceof AdminHolders.MainPanelHolder) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player admin)) return;
            if (event.getCurrentItem() == null) return;

            World world = admin.getWorld();
            int slot = event.getRawSlot();

            switch (slot) {
                case 11: // Buka Waktu Panel
                    AdminPanelGUI.openTimePanel(admin);
                    break;

                case 15: // Buka Cuaca Panel
                    AdminPanelGUI.openWeatherPanel(admin);
                    break;

                case 29: // Toggle PvP
                    boolean currentPvP = world.getPVP();
                    world.setPVP(!currentPvP);
                    admin.sendMessage(mm.deserialize(currentPvP ? "<red>PvP Global dinonaktifkan." : "<green>PvP Global diaktifkan."));
                    admin.playSound(admin.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
                    AdminPanelGUI.openMainPanel(admin);
                    break;

                case 31: // Toggle Mob Spawn
                    Boolean currentSpawn = world.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
                    boolean newState = currentSpawn == null || !currentSpawn;
                    world.setGameRule(GameRule.DO_MOB_SPAWNING, newState);
                    admin.sendMessage(mm.deserialize(newState ? "<green>Vanilla Mob Spawning diaktifkan." : "<red>Vanilla Mob Spawning dinonaktifkan."));
                    admin.playSound(admin.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
                    AdminPanelGUI.openMainPanel(admin);
                    break;

                case 33: // Toggle Chat
                    ServerState.isChatMuted = !ServerState.isChatMuted;
                    admin.sendMessage(mm.deserialize(ServerState.isChatMuted ? "<red>Global Chat dimatikan." : "<green>Global Chat dihidupkan."));
                    admin.playSound(admin.getLocation(), Sound.BLOCK_LEVER_CLICK, 1f, 1f);
                    AdminPanelGUI.openMainPanel(admin);
                    break;

                case 49: // Toggle Maintenance
                    ServerState.isMaintenance = !ServerState.isMaintenance;

                    // Simpan status ke config.yml bawaan server secara real-time
                    Vitaemanager plugin = Vitaemanager.getInstance();
                    plugin.getConfig().set("maintenance_mode", ServerState.isMaintenance);
                    plugin.saveConfig();

                    if (ServerState.isMaintenance) {
                        admin.sendMessage(mm.deserialize("<green>Mode Maintenance DIAKTIFKAN. Mengeluarkan player biasa..."));

                        // Menendang semua player yang tidak memiliki OP atau Permission
                        for (Player target : Bukkit.getOnlinePlayers()) {
                            if (!target.isOp() && !target.hasPermission("vitae.admin.maintenance")) {
                                target.kick(mm.deserialize("<red><bold>SERVER MAINTENANCE</bold>\n\n<gray>Server sedang dalam perbaikan.\nHarap kembali lagi nanti!"));
                            }
                        }
                    } else {
                        admin.sendMessage(mm.deserialize("<red>Mode Maintenance DINONAKTIFKAN. Server kembali publik."));
                    }

                    admin.playSound(admin.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
                    AdminPanelGUI.openMainPanel(admin); // Segarkan UI
                    break;
            }
        }
    }
}