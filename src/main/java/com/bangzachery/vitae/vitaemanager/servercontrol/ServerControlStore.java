package com.bangzachery.vitae.vitaemanager.servercontrol;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerControlStore {
    private final Path file;

    public ServerControlStore(Path file) {
        this.file = file.toAbsolutePath();
    }

    public ServerControlState initialize(Path legacyConfig)
            throws IOException, InvalidConfigurationException {
        if (!Files.notExists(file)) return load();

        YamlConfiguration legacy = read(legacyConfig);
        Object value = legacy.get("maintenance_mode");

        if (value != null && !(value instanceof Boolean)) {
            throw new IllegalArgumentException(
                    legacyConfig + ": maintenance_mode harus boolean");
        }

        ServerControlState initial = new ServerControlState(
                Boolean.TRUE.equals(value), false, Map.of());

        save(initial);
        return initial;
    }

    public ServerControlState load()
            throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = read(file);
        Object version = yaml.get("state-version");

        if (!(version instanceof Integer number) || number != 1) {
            throw invalid("state-version", "harus integer 1");
        }

        boolean maintenance = bool(yaml, "maintenance");
        boolean muted = bool(yaml, "chat-muted");
        Map<UUID, ServerControlState.WorldRules> worlds = new HashMap<>();

        Object section = yaml.get("worlds");

        if (section != null && !(section instanceof ConfigurationSection)) {
            throw invalid("worlds", "harus section YAML");
        }

        if (section instanceof ConfigurationSection entries) {
            for (String key : entries.getKeys(false)) {
                UUID id;

                try {
                    id = UUID.fromString(key);
                    if (!id.toString().equalsIgnoreCase(key)) {
                        throw new IllegalArgumentException();
                    }
                } catch (IllegalArgumentException exception) {
                    throw invalid("worlds." + key, "UUID dunia tidak valid");
                }

                String base = "worlds." + key;

                worlds.put(id, new ServerControlState.WorldRules(
                        bool(yaml, base + ".pvp"),
                        bool(yaml, base + ".mob-spawning")));
            }
        }

        return new ServerControlState(maintenance, muted, worlds);
    }

    // Setelah startup, service memanggil ini melalui satu thread writer.
    public void save(ServerControlState state) throws IOException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("state-version", 1);
        yaml.set("maintenance", state.maintenance());
        yaml.set("chat-muted", state.chatMuted());
        yaml.createSection("worlds");

        state.worlds().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String base = "worlds." + entry.getKey();
                    yaml.set(base + ".pvp", entry.getValue().pvp());
                    yaml.set(base + ".mob-spawning",
                            entry.getValue().mobSpawning());
                });

        Files.createDirectories(file.getParent());

        Path temporary = Files.createTempFile(
                file.getParent(), "server-control-", ".tmp");

        try {
            byte[] bytes = yaml.saveToString()
                    .getBytes(StandardCharsets.UTF_8);

            try (FileChannel channel = FileChannel.open(
                    temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);

                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }

                channel.force(true);
            }

            Files.move(temporary, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private YamlConfiguration read(Path path)
            throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(
                Files.readString(path, StandardCharsets.UTF_8));
        return yaml;
    }

    private boolean bool(YamlConfiguration yaml, String path) {
        Object value = yaml.get(path);

        if (!(value instanceof Boolean enabled)) {
            throw invalid(path, "harus boolean");
        }

        return enabled;
    }

    private IllegalArgumentException invalid(String path, String reason) {
        return new IllegalArgumentException(
                file + ": " + path + " - " + reason);
    }
}