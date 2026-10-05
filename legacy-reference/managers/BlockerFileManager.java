package com.bangzachery.vitae.vitaemanager.managers;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class BlockerFileManager {
    private static File folder;
    private static File file;
    private static FileConfiguration config;
    private static Plugin plugin;

    public static void init(Plugin p) {
        plugin = p;

        // Membuat folder khusus "blocker" di dalam folder vitaemanager
        folder = new File(plugin.getDataFolder(), "blocker");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Membuat file blocked.yml di dalam folder blocker
        file = new File(folder, "blocked.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Gagal membuat file blocked.yml!");
            }
        }

        config = YamlConfiguration.loadConfiguration(file);
    }

    public static FileConfiguration getConfig() {
        return config;
    }

    public static void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan file blocked.yml!");
        }
    }

    public static void reload() {
        config = YamlConfiguration.loadConfiguration(file);
    }
}