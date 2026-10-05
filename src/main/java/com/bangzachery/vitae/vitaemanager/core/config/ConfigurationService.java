package com.bangzachery.vitae.vitaemanager.core.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ConfigurationService {

    private final Path file;
    private final String defaultText;
    private final YamlConfiguration defaults = new YamlConfiguration();
    private final MiniMessage miniMessage = MiniMessage.builder().strict(true).build();
    private volatile Snapshot current;

    public ConfigurationService(Path file, String defaultText)
            throws InvalidConfigurationException {
        this.file = Objects.requireNonNull(file).toAbsolutePath();
        this.defaultText = Objects.requireNonNull(defaultText);
        defaults.loadFromString(defaultText);
    }

    public void initialize() throws IOException, InvalidConfigurationException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, defaultText, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        reload();
    }

    // Call on the server thread. No file or mutable YAML is exposed to consumers.
    public void reload() throws IOException, InvalidConfigurationException {
        YamlConfiguration candidate = new YamlConfiguration();
        candidate.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        candidate.setDefaults(defaults);

        requireSection(candidate, "core");
        requireSection(candidate, "messages");
        Object version = candidate.get("config-version");
        if (!(version instanceof Integer number) || number != 1) {
            throw invalid("config-version", "harus integer 1");
        }
        Object debug = candidate.get("core.debug");
        if (!(debug instanceof Boolean enabled)) {
            throw invalid("core.debug", "harus true atau false");
        }

        ConfigurationSection messageDefaults = defaults.getConfigurationSection("messages");
        if (messageDefaults == null) {
            throw invalid("messages", "resource default tidak memiliki section messages");
        }
        Map<String, Component> messages = new HashMap<>();
        for (String key : messageDefaults.getKeys(false)) {
            String path = "messages." + key;
            Object value = candidate.get(path);
            if (!(value instanceof String text)) {
                throw invalid(path, "harus berupa teks MiniMessage");
            }
            try {
                messages.put(key, miniMessage.deserialize(text));
            } catch (ParsingException | IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        file + ": " + path + " - format MiniMessage tidak valid", exception);
            }
        }

        // Commit only after every value passes validation; failures keep the old snapshot.
        if (candidate.isSet("roleplay")) requireSection(candidate, "roleplay");
        RoleplaySettings roleplay = new RoleplaySettings(
                integer(candidate, "roleplay.text-duration-ticks", 200, 20, 1200),
                integer(candidate, "roleplay.ooc-radius", 30, 1, 128),
                integer(candidate, "roleplay.max-text-length", 256, 16, 512),
                integer(candidate, "roleplay.carry-distance", 2, 1, 6));
        if (candidate.isSet("totem")) requireSection(candidate, "totem");
        TotemSettings totem = new TotemSettings(
                integer(candidate, "totem.limit", 3, 1, 1000),
                integer(candidate, "totem.cooldown-hours", 15, 0, 8760),
                bool(candidate, "totem.block-vanilla-resurrection", true));
        if (candidate.isSet("items")) requireSection(candidate, "items");
        if (candidate.isSet("integrations")) requireSection(candidate, "integrations");
        if (candidate.isSet("integrations.essentials")) requireSection(candidate, "integrations.essentials");
        ItemSettings items = new ItemSettings(bool(candidate, "items.sneak-to-pickup", true),
                bool(candidate, "integrations.essentials.block-economy-commands", true));
        current = new Snapshot(enabled, messages, roleplay, totem, items);
    }

    public Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Konfigurasi belum diinisialisasi");
        }
        return snapshot;
    }

    private void requireSection(YamlConfiguration configuration, String path) {
        Object value = configuration.get(path);
        if (!(value instanceof ConfigurationSection)) {
            throw invalid(path, "harus berupa section YAML");
        }
    }

    private IllegalArgumentException invalid(String path, String reason) {
        return new IllegalArgumentException(file + ": " + path + " - " + reason);
    }

    private int integer(YamlConfiguration configuration, String path, int fallback, int min, int max) {
        Object value = configuration.get(path, fallback);
        if (!(value instanceof Integer number) || number < min || number > max) {
            throw invalid(path, "harus integer " + min + " sampai " + max);
        }
        return number;
    }

    public record RoleplaySettings(int durationTicks, int oocRadius, int maxTextLength, int carryDistance) { }

    private boolean bool(YamlConfiguration configuration, String path, boolean fallback) {
        Object value = configuration.get(path, fallback);
        if (!(value instanceof Boolean enabled)) throw invalid(path, "harus true atau false");
        return enabled;
    }

    public record TotemSettings(int limit, int cooldownHours, boolean blockVanillaResurrection) { }

    public record ItemSettings(boolean sneakToPickup, boolean blockEssentialsEconomy) { }

    public record Snapshot(boolean debug, Map<String, Component> messages,
                           RoleplaySettings roleplay, TotemSettings totem, ItemSettings items) {
        public Snapshot {
            messages = Map.copyOf(messages);
        }
    }
}