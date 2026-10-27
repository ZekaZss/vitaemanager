package com.bangzachery.vitae.vitaemanager.integration;

import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** API references stay in a separately loaded implementation, preserving operation without PAPI. */
public final class PlaceholderHook implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final VitiService viti;
    private Runnable unregister;
    public PlaceholderHook(JavaPlugin plugin, VitiService viti) { this.plugin = plugin; this.viti = viti; }
    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        register();
    }
    private void register() {
        if (unregister != null || !plugin.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) return;
        try {
            var expansion = new VitiPlaceholders(plugin, viti);
            if (expansion.register()) unregister = expansion::unregister;
            else plugin.getLogger().warning("Placeholder vitae gagal didaftarkan; periksa konflik identifier PlaceholderAPI.");
        } catch (LinkageError | RuntimeException error) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "Integrasi PlaceholderAPI tidak tersedia; ekonomi tetap aktif.", error);
        }
    }
    @EventHandler public void enabled(PluginEnableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) register();
    }
    @EventHandler public void disabled(PluginDisableEvent event) {
        if (event.getPlugin().getName().equals("PlaceholderAPI")) close();
    }
    @Override public void close() {
        if (unregister != null) {
            try { unregister.run(); } catch (RuntimeException | LinkageError ignored) { }
            unregister = null;
        }
    }
}
