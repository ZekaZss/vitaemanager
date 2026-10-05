package com.bangzachery.vitae.vitaemanager.listeners;

import com.bangzachery.vitae.vitaemanager.Vitaemanager;
import com.bangzachery.vitae.vitaemanager.guis.AdminPanelGUI;
import com.bangzachery.vitae.vitaemanager.guis.ProfileHolders;
import com.bangzachery.vitae.vitaemanager.guis.ProfilePanel;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ProfileListener implements Listener {

    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Vitaemanager plugin = JavaPlugin.getPlugin(Vitaemanager.class);

    @EventHandler
    public void onProfileClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        if (event.getCurrentItem() == null) return;

        // 1. Logika GUI Daftar Player
        if (event.getInventory().getHolder() instanceof ProfileHolders.OnlinePlayersHolder holder) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            if (slot == 45) {
                AdminPanelGUI.openMainPanel(admin);
            } else if (slot == 48) {
                ProfilePanel.openOnlinePlayers(admin, holder.getPage() - 1);
            } else if (slot == 53) {
                ProfilePanel.openOnlinePlayers(admin, holder.getPage() + 1);
            } else if (event.getCurrentItem().getType() == Material.PLAYER_HEAD) {
                SkullMeta meta = (SkullMeta) event.getCurrentItem().getItemMeta();
                if (meta != null && meta.getOwningPlayer() instanceof Player target && target.isOnline()) {
                    ProfilePanel.openPlayerProfile(admin, target);
                } else {
                    admin.sendMessage(mm.deserialize("<red>Player tersebut sudah offline."));
                }
            }
        }
        // 2. Logika GUI Profil Player
        else if (event.getInventory().getHolder() instanceof ProfileHolders.PlayerProfileHolder holder) {
            event.setCancelled(true);
            Player target = holder.getTarget();
            if (target == null || !target.isOnline()) {
                admin.sendMessage(mm.deserialize("<red>Player sudah offline."));
                admin.closeInventory();
                return;
            }

            int slot = event.getRawSlot();
            switch (slot) {
                case 0: // Heal
                    Bukkit.dispatchCommand(admin, "heal " + target.getName());
                    break;
                case 3: // Revive
                    Bukkit.dispatchCommand(admin, "revive revive " + target.getName());
                    break;
                case 4: // Gamemode Cycle
                    GameMode next = switch (target.getGameMode()) {
                        case SURVIVAL -> GameMode.CREATIVE;
                        case CREATIVE -> GameMode.ADVENTURE;
                        case ADVENTURE -> GameMode.SPECTATOR;
                        case SPECTATOR -> GameMode.SURVIVAL;
                    };
                    target.setGameMode(next);
                    admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    ProfilePanel.openPlayerProfile(admin, target); // Refresh GUI
                    break;
                case 6: // TP To
                    admin.teleportAsync(target.getLocation());
                    admin.sendMessage(mm.deserialize("<green>Teleportasi ke " + target.getName()));
                    break;
                case 8: // TP Here
                    target.teleportAsync(admin.getLocation());
                    admin.sendMessage(mm.deserialize("<green>Menarik " + target.getName() + " ke lokasimu."));
                    break;
                case 12: // Feed
                    target.setFoodLevel(20);
                    target.setSaturation(20f);
                    admin.sendMessage(mm.deserialize("<green>Player " + target.getName() + " telah dikenyangkan."));
                    break;
                case 14: // Invsee
                    ProfilePanel.openCustomInvsee(admin, target);
                    break;
                case 22: // Kembali
                    ProfilePanel.openOnlinePlayers(admin, 0);
                    break;
            }
        }
        // 3. Logika Custom Invsee (Anti-Dupe & Sync)
        else if (event.getInventory().getHolder() instanceof ProfileHolders.CustomInvseeHolder holder) {
            Player target = holder.getTarget();
            int slot = event.getRawSlot();

            // Kaca hitam (50, 51, 52) tidak bisa dipindah
            if (slot >= 50 && slot <= 52) {
                event.setCancelled(true);
                return;
            }

            // Tombol kembali (53)
            if (slot == 53) {
                event.setCancelled(true);
                if (target != null && target.isOnline()) {
                    ProfilePanel.openPlayerProfile(admin, target);
                } else {
                    admin.closeInventory();
                }
                return;
            }

            // Sync data tas jika admin naruh/ambil barang
            Bukkit.getScheduler().runTask(plugin, () -> syncInventory(holder, event.getInventory()));
        }
    }

    // Mencegah dupe saat admin menggunakan Drag di Custom Invsee
    @EventHandler
    public void onInvseeDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ProfileHolders.CustomInvseeHolder holder) {
            Bukkit.getScheduler().runTask(plugin, () -> syncInventory(holder, event.getInventory()));
        }
    }

    // Fungsi canggih untuk mensinkronisasi Custom GUI langsung ke badan player
    private void syncInventory(ProfileHolders.CustomInvseeHolder holder, Inventory gui) {
        Player target = holder.getTarget();
        if (target == null || !target.isOnline()) return;

        for (int i = 0; i < 36; i++) {
            target.getInventory().setItem(i, gui.getItem(i));
        }
        target.getInventory().setHelmet(gui.getItem(45));
        target.getInventory().setChestplate(gui.getItem(46));
        target.getInventory().setLeggings(gui.getItem(47));
        target.getInventory().setBoots(gui.getItem(48));
        target.getInventory().setItemInOffHand(gui.getItem(49));
    }
}