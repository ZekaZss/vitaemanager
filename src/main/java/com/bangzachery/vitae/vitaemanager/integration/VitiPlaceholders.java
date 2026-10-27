package com.bangzachery.vitae.vitaemanager.integration;

import com.bangzachery.vitae.vitaemanager.economy.VitiService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/** Only loaded by PlaceholderHook after the optional API is enabled. No world/file access on request. */
public final class VitiPlaceholders extends PlaceholderExpansion {
    private final JavaPlugin plugin;
    private final VitiService viti;
    public VitiPlaceholders(JavaPlugin plugin, VitiService viti) { this.plugin = plugin; this.viti = viti; }
    @Override public @NotNull String getIdentifier() { return "vitae"; }
    @Override public @NotNull String getAuthor() { return String.join(", ", plugin.getPluginMeta().getAuthors()); }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player, @NotNull String params) {
        return VitiPlaceholderValues.resolve(viti.current(), player == null ? null : player.getUniqueId(), params);
    }
}
