package com.bangzachery.vitae.vitaemanager.guis;

import com.bangzachery.vitae.vitaemanager.managers.ServerState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelGUI {
    private static final MiniMessage mm = MiniMessage.miniMessage();

    // 1. PANEL UTAMA (54 Slot)
    public static void openMainPanel(Player player) {
        AdminHolders.MainPanelHolder holder = new AdminHolders.MainPanelHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, mm.deserialize("<dark_gray>Panel Administrator"));
        holder.setInventory(inv);

        World world = player.getWorld();
        fillEmptyWithGlass(inv);

        // Slot 11: Pengatur Waktu (Per-World)
        inv.setItem(11, createItem(Material.CLOCK, "<yellow>Siklus Waktu",
                "<gray>Dunia: <white>" + world.getName(), "<gray>Klik untuk mengatur waktu."));

        // Slot 15: Pengatur Cuaca (Per-World)
        inv.setItem(15, createItem(Material.SUNFLOWER, "<aqua>Siklus Cuaca",
                "<gray>Dunia: <white>" + world.getName(), "<gray>Klik untuk mengatur cuaca."));

        // Slot 29: Mode PvP
        boolean isPvP = world.getPVP();
        inv.setItem(29, createItem(Material.DIAMOND_SWORD, "<red>Mode PvP",
                "<gray>Status: " + (isPvP ? "<green>ON" : "<red>OFF"),
                "<gray>Klik untuk toggle PvP."));

        // Slot 31: Mob Spawning
        Boolean mobSpawn = world.getGameRuleValue(GameRule.DO_MOB_SPAWNING);
        boolean isMobOn = (mobSpawn != null && mobSpawn);
        inv.setItem(31, createItem(Material.ZOMBIE_HEAD, "<green>Mob Spawning",
                "<gray>Status: " + (isMobOn ? "<green>NORMAL" : "<red>PEACEFUL"),
                "<gray>Klik untuk toggle Monster."));

        // Slot 33: Chat Toggle
        inv.setItem(33, createItem(Material.PAPER, "<white>Global Chat",
                "<gray>Status: " + (ServerState.isChatMuted ? "<red>OFF (Muted)" : "<green>ON"),
                "<gray>Klik untuk toggle chat."));

        // Slot 49: Maintenance Toggle
        inv.setItem(49, createItem(Material.BARRIER, "<dark_red>Maintenance Mode",
                "<gray>Status: " + (ServerState.isMaintenance ? "<green>ON (Terkunci)" : "<red>OFF (Bebas Masuk)"),
                "<gray>Klik untuk toggle maintenance."));

        player.openInventory(inv);
    }

    // 2. PANEL WAKTU (9 Slot - Per World)
    public static void openTimePanel(Player player) {
        AdminHolders.TimePanelHolder holder = new AdminHolders.TimePanelHolder();
        Inventory inv = Bukkit.createInventory(holder, 9, mm.deserialize("<yellow>Waktu: " + player.getWorld().getName()));
        holder.setInventory(inv);

        fillEmptyWithGlass(inv);

        inv.setItem(0, createItem(Material.LIGHT_BLUE_DYE, "<yellow>Pagi", "<gray>Set ke 06:00"));
        inv.setItem(2, createItem(Material.YELLOW_DYE, "<gold>Siang", "<gray>Set ke 12:00"));
        inv.setItem(4, createItem(Material.ORANGE_DYE, "<red>Sore", "<gray>Set ke 18:00"));
        inv.setItem(6, createItem(Material.BLUE_DYE, "<blue>Malam", "<gray>Set ke 24:00"));

        inv.setItem(8, createItem(Material.ARROW, "<red>Kembali", "<gray>Kembali ke menu utama."));
        player.openInventory(inv);
    }

    // 3. PANEL CUACA (9 Slot - Per World)
    public static void openWeatherPanel(Player player) {
        AdminHolders.WeatherPanelHolder holder = new AdminHolders.WeatherPanelHolder();
        Inventory inv = Bukkit.createInventory(holder, 9, mm.deserialize("<aqua>Cuaca: " + player.getWorld().getName()));
        holder.setInventory(inv);

        fillEmptyWithGlass(inv);

        inv.setItem(0, createItem(Material.WATER_BUCKET, "<blue>Hujan", "<gray>Membuat cuaca hujan."));
        inv.setItem(2, createItem(Material.LIGHTNING_ROD, "<yellow>Petir", "<gray>Memunculkan petir & hentikan hujan."));
        inv.setItem(4, createItem(Material.TRIDENT, "<dark_aqua>Badai Petir", "<gray>Hujan lebat + Petir."));
        inv.setItem(6, createItem(Material.SUNFLOWER, "<gold>Cerah", "<gray>Hentikan hujan dan badai."));

        inv.setItem(8, createItem(Material.ARROW, "<red>Kembali", "<gray>Kembali ke menu utama."));
        player.openInventory(inv);
    }

    private static void fillEmptyWithGlass(Inventory inv) {
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "<black> ");
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null || inv.getItem(i).getType() == Material.AIR) {
                inv.setItem(i, bg);
            }
        }
    }

    private static ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false));
            if (loreLines.length > 0) {
                List<Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(mm.deserialize(line).decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}