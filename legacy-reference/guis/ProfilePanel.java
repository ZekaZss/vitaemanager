package com.bangzachery.vitae.vitaemanager.guis;

import com.bangzachery.vitae.vitaemanager.managers.PDCManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class ProfilePanel {

    private static final MiniMessage mm = MiniMessage.miniMessage();

    public static void openOnlinePlayers(Player admin, int page) {
        ProfileHolders.OnlinePlayersHolder holder = new ProfileHolders.OnlinePlayersHolder(page);
        Inventory inv = Bukkit.createInventory(holder, 54, mm.deserialize("<dark_gray>» <green>Player Online (Hal " + (page + 1) + ") <dark_gray>«"));
        holder.setInventory(inv);

        List<Player> players = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            // Sembunyikan player yang sedang vanish dari GUI agar admin bisa memantau diam-diam
            if (p.hasMetadata("vanished") && p.getMetadata("vanished").get(0).asBoolean()) {
                continue;
            }
            players.add(p);
        }

        int maxPlayersPerPage = 45;
        int startIndex = page * maxPlayersPerPage;
        int endIndex = Math.min(startIndex + maxPlayersPerPage, players.size());

        for (int i = startIndex; i < endIndex; i++) {
            Player target = players.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(target);
                meta.displayName(mm.deserialize("<yellow>" + target.getName()).decoration(TextDecoration.ITALIC, false));

                List<Component> lore = new ArrayList<>();
                lore.add(mm.deserialize("<gray>Darah: <red>" + Math.round(target.getHealth()) + " HP").decoration(TextDecoration.ITALIC, false));

                boolean isDown = target.isDead() || target.getHealth() < 2.0;
                lore.add(mm.deserialize("<gray>Status: " + (isDown ? "<red>DOWN" : "<green>NO DOWN")).decoration(TextDecoration.ITALIC, false));
                lore.add(mm.deserialize(""));
                lore.add(mm.deserialize("<white>Klik untuk buka profil!").decoration(TextDecoration.ITALIC, false));

                meta.lore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(i - startIndex, head);
        }

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "<black> ");
        for (int i = 45; i < 54; i++) inv.setItem(i, bg);

        inv.setItem(45, createItem(Material.BARRIER, "<red>Kembali ke Admin Panel"));
        if (page > 0) inv.setItem(48, createItem(Material.ARROW, "<yellow>Halaman Sebelumnya"));
        if (endIndex < players.size()) inv.setItem(53, createItem(Material.ARROW, "<yellow>Halaman Berikutnya"));

        admin.openInventory(inv);
    }

    public static void openPlayerProfile(Player admin, Player target) {
        ProfileHolders.PlayerProfileHolder holder = new ProfileHolders.PlayerProfileHolder(target);
        Inventory inv = Bukkit.createInventory(holder, 27, mm.deserialize("<dark_gray>» <yellow>Profil: " + target.getName() + " <dark_gray>«"));
        holder.setInventory(inv);

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "<black> ");
        for (int i = 0; i < 27; i++) inv.setItem(i, bg);

        inv.setItem(0, createItem(Material.GLISTERING_MELON_SLICE, "<light_purple>Heal Player", "<gray>Menjalankan: /heal " + target.getName()));
        inv.setItem(3, createItem(Material.TOTEM_OF_UNDYING, "<gold>Revive Player", "<gray>Menjalankan: /revive revive " + target.getName()));

        String gm = target.getGameMode().name();
        inv.setItem(4, createItem(Material.COMMAND_BLOCK, "<aqua>Ubah GameMode", "<gray>Saat ini: <white>" + gm, "<gray>Klik untuk mengganti."));

        inv.setItem(6, createItem(Material.ENDER_PEARL, "<green>Teleport (Ke Player)", "<gray>Klik untuk TP ke " + target.getName()));
        inv.setItem(8, createItem(Material.CHORUS_FRUIT, "<dark_green>Teleport (Tarik Player)", "<gray>Klik untuk menarik " + target.getName() + " ke sini."));

        double vitiBalance = PDCManager.getViti(target);
        inv.setItem(10, createItem(Material.GOLD_INGOT, "<yellow>Saldo Viti", "<gray>Total: <white>" + vitiBalance + " Viti"));

        inv.setItem(12, createItem(Material.COOKED_BEEF, "<gold>Feed (Kenyang)", "<gray>Klik untuk memulihkan rasa lapar."));
        inv.setItem(14, createItem(Material.CHEST, "<gold>Invsee (Lihat Inventory)", "<gray>Klik untuk membuka tas player ini."));

        inv.setItem(16, createItem(Material.EXPERIENCE_BOTTLE, "<aqua>Rank AuraSkills", "<gray>Cek rank/level skill player ini."));

        inv.setItem(22, createItem(Material.ARROW, "<red>Kembali", "<gray>Kembali ke Daftar Player"));

        admin.openInventory(inv);
    }

    public static void openCustomInvsee(Player admin, Player target) {
        ProfileHolders.CustomInvseeHolder holder = new ProfileHolders.CustomInvseeHolder(target);
        Inventory inv = Bukkit.createInventory(holder, 54, mm.deserialize("<dark_gray>» <gold>Tas: " + target.getName() + " <dark_gray>«"));
        holder.setInventory(inv);

        ItemStack[] mainContents = target.getInventory().getStorageContents();
        for (int i = 0; i < mainContents.length; i++) {
            if (mainContents[i] != null) inv.setItem(i, mainContents[i].clone());
        }

        if (target.getInventory().getHelmet() != null) inv.setItem(45, target.getInventory().getHelmet().clone());
        if (target.getInventory().getChestplate() != null) inv.setItem(46, target.getInventory().getChestplate().clone());
        if (target.getInventory().getLeggings() != null) inv.setItem(47, target.getInventory().getLeggings().clone());
        if (target.getInventory().getBoots() != null) inv.setItem(48, target.getInventory().getBoots().clone());
        if (target.getInventory().getItemInOffHand() != null) inv.setItem(49, target.getInventory().getItemInOffHand().clone());

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, "<black> ");
        inv.setItem(50, bg); inv.setItem(51, bg); inv.setItem(52, bg);
        inv.setItem(53, createItem(Material.ARROW, "<red>Kembali", "<gray>Kembali ke Profil Player"));

        admin.openInventory(inv);
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