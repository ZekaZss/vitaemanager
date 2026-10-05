package com.bangzachery.vitae.vitaemanager.managers;

import com.bangzachery.vitae.vitaemanager.utils.Keys;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class VitiFileManager {
    private static File file;
    private static FileConfiguration config;
    private static Plugin plugin;

    public static void init(Plugin p) {
        plugin = p;
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }
        file = new File(plugin.getDataFolder(), "viti.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Gagal membuat viti.yml!");
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    // Memuat ulang (reload) file dan menyinkronkan saldo baru ke player yang sedang online
    public static void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        for (Player p : Bukkit.getOnlinePlayers()) {
            syncFileToPDC(p);
        }
    }

    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan viti.yml!");
        }
    }

    // Sinkronisasi data dua arah (Dari RAM ke File)
    public static void updatePlayer(Player player, double currentBalance) {
        String uuid = player.getUniqueId().toString();
        String path = "players." + uuid;

        config.set(path + ".name", player.getName());
        config.set(path + ".balance", currentBalance);

        // Sistem Rekor (Highest Balance)
        double highest = config.getDouble(path + ".highest_balance", 0.0);
        if (currentBalance > highest) {
            config.set(path + ".highest_balance", currentBalance);
        }
        save();
    }

    // Sinkronisasi data dua arah (Dari File ke RAM) dipanggil saat player login
    public static void syncFileToPDC(Player player) {
        String uuid = player.getUniqueId().toString();
        String path = "players." + uuid;

        if (config.contains(path + ".balance")) {
            double fileBalance = config.getDouble(path + ".balance");
            // Set langsung ke PDC secara diam-diam agar tidak memicu loop penyimpanan file
            player.getPersistentDataContainer().set(Keys.VITI, PersistentDataType.DOUBLE, fileBalance);
        } else {
            // Jika pemain baru, daftarkan ia ke dalam viti.yml
            updatePlayer(player, PDCManager.getViti(player));
        }
    }

    // Algoritma O(N) untuk mengambil 10 Sultan Terkaya berdasarkan Rekor Tertinggi
    public static List<Map.Entry<String, Double>> getTop10Highest() {
        ConfigurationSection players = config.getConfigurationSection("players");
        if (players == null) return new ArrayList<>();

        List<Map.Entry<String, Double>> list = new ArrayList<>();
        for (String uuid : players.getKeys(false)) {
            String name = players.getString(uuid + ".name", "Unknown");
            double highest = players.getDouble(uuid + ".highest_balance", 0.0);
            list.add(new AbstractMap.SimpleEntry<>(name, highest));
        }

        // Urutkan dari yang terbesar ke terkecil
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return list.size() > 10 ? list.subList(0, 10) : list;
    }
}