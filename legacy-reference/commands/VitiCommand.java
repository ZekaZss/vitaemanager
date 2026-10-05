package com.bangzachery.vitae.vitaemanager.commands;

import com.bangzachery.vitae.vitaemanager.managers.PDCManager;
import com.bangzachery.vitae.vitaemanager.managers.VitiFileManager;
import com.bangzachery.vitae.vitaemanager.utils.Keys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class VitiCommand implements CommandExecutor, TabCompleter {

    private final MiniMessage mm = MiniMessage.miniMessage();

    public static String formatViti(double amount) {
        NumberFormat format = NumberFormat.getInstance(new Locale("id", "ID"));
        if (amount == (long) amount) {
            return format.format((long) amount);
        }
        return format.format(amount);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini hanya bisa digunakan di dalam game oleh player.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("lihat")) {
            double bal = PDCManager.getViti(player);
            player.sendMessage(mm.deserialize("<gold>Saldo Viti kamu saat ini: <white>" + formatViti(bal)));
            return true;
        }

        // --- COMMAND BARU: /VITI BOARD ---
        if (args[0].equalsIgnoreCase("board")) {
            List<Map.Entry<String, Double>> top = VitiFileManager.getTop10Highest();
            player.sendMessage(mm.deserialize("<gold><bold>=== TOP 10 SULTAN VITAE ===</bold>"));
            if (top.isEmpty()) {
                player.sendMessage(mm.deserialize("<gray>Belum ada data Sultan yang tercatat."));
            } else {
                int rank = 1;
                for (Map.Entry<String, Double> entry : top) {
                    player.sendMessage(mm.deserialize("<yellow>" + rank + ". <white>" + entry.getKey() + " <gray>- <green>" + formatViti(entry.getValue()) + " Viti"));
                    rank++;
                }
            }
            return true;
        }

        // --- COMMAND BARU: /VITI RELOAD ---
        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("vitae.admin.viti")) {
                player.sendMessage(mm.deserialize("<red>Kamu tidak punya izin untuk melakukan itu."));
                return true;
            }
            VitiFileManager.reload();
            player.sendMessage(mm.deserialize("<green>File viti.yml berhasil dimuat ulang dan disinkronisasikan ke semua pemain!"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            return true;
        }

        // --- COMMAND: /VITI BERI ---
        if (args[0].equalsIgnoreCase("beri")) {
            if (args.length < 2) {
                player.sendMessage(mm.deserialize("<red>Gunakan: /viti beri <nominal>"));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1].replace(".", ""));
            } catch (NumberFormatException e) {
                player.sendMessage(mm.deserialize("<red>Nominal harus berupa angka valid!"));
                return true;
            }

            if (amount <= 0) {
                player.sendMessage(mm.deserialize("<red>Nominal harus lebih dari 0."));
                return true;
            }

            double senderBalance = PDCManager.getViti(player);
            if (senderBalance < amount) {
                player.sendMessage(mm.deserialize("<red>Viti kamu tidak cukup untuk transaksi ini."));
                return true;
            }

            Entity entityInFront = player.getTargetEntity(2, false);
            if (!(entityInFront instanceof Player targetLooking) ||
                    player.getLocation().distance(targetLooking.getLocation()) > 1.5) {

                player.sendMessage(mm.deserialize("<red>Kamu tidak bisa beri Viti karena tidak ada player tepat di hadapanmu (maksimal 1.5 blok)."));
                return true;
            }

            PDCManager.removeViti(player, amount);
            PDCManager.addViti(targetLooking, amount);

            player.sendMessage(mm.deserialize("<green>Kamu berhasil memberikan <white>" + formatViti(amount) + " Viti <green>kepada " + targetLooking.getName()));
            targetLooking.sendMessage(mm.deserialize("<green>Kamu menerima <white>" + formatViti(amount) + " Viti <green>dari " + player.getName()));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            return true;
        }

        // --- COMMAND: /VITI CONVERT ---
        if (args[0].equalsIgnoreCase("convert")) {
            if (args.length < 2) {
                player.sendMessage(mm.deserialize("<red>Gunakan: /viti convert <nominal>"));
                return true;
            }

            double amount;
            try {
                amount = Double.parseDouble(args[1].replace(".", ""));
            } catch (NumberFormatException e) {
                player.sendMessage(mm.deserialize("<red>Nominal harus berupa angka valid!"));
                return true;
            }

            if (amount <= 0 || PDCManager.getViti(player) < amount) {
                player.sendMessage(mm.deserialize("<red>Viti kamu tidak mencukupi untuk di-convert."));
                return true;
            }

            PDCManager.removeViti(player, amount);

            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.displayName(mm.deserialize("<gold><bold>Uang Viti Fisik").decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(mm.deserialize("<gray>Nominal: <white>" + formatViti(amount) + " Viti").decoration(TextDecoration.ITALIC, false));
            lore.add(mm.deserialize(""));
            lore.add(mm.deserialize("<yellow>» Klik Kanan untuk memasukkan ke Saldo").decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);

            meta.getPersistentDataContainer().set(Keys.VITI_PAPER, PersistentDataType.DOUBLE, amount);
            paper.setItemMeta(meta);

            player.getInventory().addItem(paper);
            player.sendMessage(mm.deserialize("<green>Berhasil meng-convert <white>" + formatViti(amount) + " Viti <green>menjadi uang fisik."));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER, 1f, 1f);
            return true;
        }

        // --- COMMAND ADMIN: ADD, SET, REMOVE ---
        if (!player.hasPermission("vitae.admin.viti")) {
            player.sendMessage(mm.deserialize("<red>Kamu tidak punya izin untuk melakukan itu."));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(mm.deserialize("<red>Gunakan: /viti " + args[0] + " <player> <nominal>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(mm.deserialize("<red>Player tidak ditemukan."));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[2].replace(".", ""));
        } catch (NumberFormatException e) {
            player.sendMessage(mm.deserialize("<red>Nominal angka tidak valid!"));
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            PDCManager.addViti(target, amount);
            player.sendMessage(mm.deserialize("<green>Berhasil menambahkan <white>" + formatViti(amount) + " Viti <green>kepada " + target.getName()));
        } else if (args[0].equalsIgnoreCase("set")) {
            PDCManager.setViti(target, amount);
            player.sendMessage(mm.deserialize("<green>Berhasil mengatur Viti <white>" + target.getName() + " <green>menjadi " + formatViti(amount)));
        } else if (args[0].equalsIgnoreCase("remove")) {
            PDCManager.removeViti(target, amount);
            player.sendMessage(mm.deserialize("<green>Berhasil memotong <white>" + formatViti(amount) + " Viti <green>dari " + target.getName()));
        }

        return true;
    }

    // --- TAB COMPLETER MECHANIC ---
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("lihat");
            completions.add("beri");
            completions.add("convert");
            completions.add("board");
            if (sender.hasPermission("vitae.admin.viti")) {
                completions.add("add");
                completions.add("set");
                completions.add("remove");
                completions.add("reload");
            }
        } else if (args.length == 2) {
            String arg0 = args[0].toLowerCase();
            if (arg0.equals("beri") || arg0.equals("convert")) {
                completions.add("<nominal>");
            }
            else if (sender.hasPermission("vitae.admin.viti") && (arg0.equals("add") || arg0.equals("set") || arg0.equals("remove"))) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    completions.add(p.getName());
                }
            }
        } else if (args.length == 3) {
            String arg0 = args[0].toLowerCase();
            if (sender.hasPermission("vitae.admin.viti") && (arg0.equals("add") || arg0.equals("set") || arg0.equals("remove"))) {
                completions.add("<nominal>");
            }
        }

        List<String> result = new ArrayList<>();
        for (String c : completions) {
            if (c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())) {
                result.add(c);
            }
        }
        return result;
    }
}