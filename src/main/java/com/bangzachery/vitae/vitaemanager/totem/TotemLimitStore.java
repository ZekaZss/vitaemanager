package com.bangzachery.vitae.vitaemanager.totem;

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

/** Only the administrative limit override lives here. Player PDC remains authoritative. */
public final class TotemLimitStore {
    private final Path file;

    public TotemLimitStore(Path file) { this.file = file.toAbsolutePath(); }

    public Integer load() throws IOException, InvalidConfigurationException {
        if (Files.notExists(file)) return null;
        var yaml = new YamlConfiguration();
        yaml.loadFromString(Files.readString(file, StandardCharsets.UTF_8));
        if (!(yaml.get("state-version") instanceof Integer version) || version != 1
                || !(yaml.get("limit") instanceof Integer limit)) {
            throw new IllegalArgumentException(file + ": state-version harus 1 dan limit harus integer");
        }
        TotemState.requireLimit(limit);
        return limit;
    }

    public void save(int limit) throws IOException {
        TotemState.requireLimit(limit);
        var yaml = new YamlConfiguration();
        yaml.set("state-version", 1);
        yaml.set("limit", limit);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "totem-settings-", ".tmp");
        try {
            ByteBuffer bytes = ByteBuffer.wrap(yaml.saveToString().getBytes(StandardCharsets.UTF_8));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
}