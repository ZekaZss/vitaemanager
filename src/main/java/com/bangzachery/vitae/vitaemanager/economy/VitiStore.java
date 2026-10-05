package com.bangzachery.vitae.vitaemanager.economy;

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
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class VitiStore {
    private final Path file;
    private String base = "";

    public VitiStore(Path file) { this.file = file.toAbsolutePath(); }

    public VitiLedger initialize() throws IOException, InvalidConfigurationException {
        if (Files.notExists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "players: {}\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        return load();
    }

    public VitiLedger load() throws IOException, InvalidConfigurationException {
        String text = Files.readString(file, StandardCharsets.UTF_8);
        YamlConfiguration yaml = yaml(text);
        Object version = yaml.get("vitae-data-version");
        if (version != null && (!(version instanceof Integer number) || number != 1)) {
            throw invalid("vitae-data-version", "versi tidak didukung");
        }
        var accounts = new HashMap<UUID, VitiLedger.Account>();
        ConfigurationSection players = section(yaml, "players");
        if (players != null) for (String key : players.getKeys(false)) {
            // The original bundled viti.yml contained this sample, not a real player UUID.
            if (key.equals("UUID-PEMAIN")) continue;
            UUID id = uuid(key);
            String path = "players." + key;
            Object name = yaml.get(path + ".name", id.toString());
            if (!(name instanceof String textName)) throw invalid(path + ".name", "harus teks");
            var balance = VitiAmount.stored(yaml.get(path + ".balance_exact", yaml.get(path + ".balance")));
            var highest = VitiAmount.stored(yaml.get(path + ".highest_balance_exact",
                    yaml.get(path + ".highest_balance", balance.toPlainString())));
            if (accounts.put(id, new VitiLedger.Account(textName, balance, highest)) != null) {
                throw invalid(path, "UUID duplikat");
            }
        }
        var notes = new HashMap<UUID, VitiLedger.Note>();
        ConfigurationSection issued = section(yaml, "vitae-notes");
        if (issued != null) for (String key : issued.getKeys(false)) {
            String path = "vitae-notes." + key;
            Object pending = yaml.get(path + ".pending");
            if (!(pending instanceof Boolean waiting)) throw invalid(path + ".pending", "harus boolean");
            Object owner = yaml.get(path + ".owner");
            if (!(owner instanceof String ownerId)) throw invalid(path + ".owner", "harus UUID");
            if (notes.put(uuid(key), new VitiLedger.Note(uuid(ownerId),
                    VitiAmount.stored(yaml.get(path + ".amount")), waiting)) != null) {
                throw invalid(path, "UUID duplikat");
            }
        }
        var redeemed = new HashSet<UUID>();
        Object receipts = yaml.get("vitae-redeemed");
        if (receipts != null) {
            if (!(receipts instanceof java.util.List<?> list)) throw invalid("vitae-redeemed", "harus list UUID");
            for (Object item : list) {
                if (!(item instanceof String id)) throw invalid("vitae-redeemed", "harus list UUID");
                redeemed.add(uuid(id));
            }
        }
        VitiLedger.Giveaway giveaway = null;
        ConfigurationSection round = section(yaml, "vitae-giveaway");
        if (round != null) {
            Object roundId = round.get("id"), active = round.get("active"), recipients = round.get("claimed");
            if (!(roundId instanceof String id)) throw invalid("vitae-giveaway.id", "harus UUID");
            if (!(active instanceof Boolean enabled)) throw invalid("vitae-giveaway.active", "harus boolean");
            if (!(recipients instanceof java.util.List<?> list)) {
                throw invalid("vitae-giveaway.claimed", "harus list UUID");
            }
            var claimed = new HashSet<UUID>();
            for (Object item : list) {
                if (!(item instanceof String player) || !claimed.add(uuid(player))) {
                    throw invalid("vitae-giveaway.claimed", "UUID tidak valid atau duplikat");
                }
            }
            giveaway = new VitiLedger.Giveaway(uuid(id), VitiAmount.stored(round.get("amount")), claimed, enabled);
        }
        VitiLedger result = new VitiLedger(accounts, notes, redeemed, giveaway);
        base = text;
        return result;
    }

    public void save(VitiLedger ledger) throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = yaml(base);
        Path backup = file.resolveSibling("viti.yml.before-remake");
        if (yaml.get("vitae-data-version") == null && Files.notExists(backup)) {
            Files.copy(file, backup);
        }
        var originalKeys = new HashMap<UUID, String>();
        ConfigurationSection players = section(yaml, "players");
        if (players != null) for (String key : players.getKeys(false)) {
            if (!key.equals("UUID-PEMAIN")) originalKeys.put(uuid(key), key);
        }
        yaml.set("vitae-data-version", 1);
        ledger.accounts().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String path = "players." + originalKeys.getOrDefault(entry.getKey(), entry.getKey().toString());
            yaml.set(path + ".name", entry.getValue().name());
            yaml.set(path + ".balance", entry.getValue().balance().doubleValue());
            yaml.set(path + ".balance_exact", entry.getValue().balance().toPlainString());
            yaml.set(path + ".highest_balance", entry.getValue().highest().doubleValue());
            yaml.set(path + ".highest_balance_exact", entry.getValue().highest().toPlainString());
        });
        yaml.set("vitae-notes", null);
        yaml.createSection("vitae-notes");
        ledger.notes().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String path = "vitae-notes." + entry.getKey();
            yaml.set(path + ".owner", entry.getValue().owner().toString());
            yaml.set(path + ".amount", entry.getValue().amount().toPlainString());
            yaml.set(path + ".pending", entry.getValue().pending());
        });
        yaml.set("vitae-redeemed", ledger.redeemed().stream().sorted().map(UUID::toString).toList());
        if (ledger.giveaway() == null) yaml.set("vitae-giveaway", null);
        else {
            var round = ledger.giveaway();
            yaml.set("vitae-giveaway.id", round.id().toString());
            yaml.set("vitae-giveaway.amount", round.amount().toPlainString());
            yaml.set("vitae-giveaway.active", round.active());
            yaml.set("vitae-giveaway.claimed", round.claimed().stream().sorted().map(UUID::toString).toList());
        }
        String output = yaml.saveToString();
        Path temporary = Files.createTempFile(file.getParent(), "viti-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer data = ByteBuffer.wrap(output.getBytes(StandardCharsets.UTF_8));
                while (data.hasRemaining()) channel.write(data);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            base = output;
        } finally { Files.deleteIfExists(temporary); }
    }

    private YamlConfiguration yaml(String text) throws InvalidConfigurationException {
        var yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }

    private ConfigurationSection section(YamlConfiguration yaml, String path) {
        Object value = yaml.get(path);
        if (value == null) return null;
        if (!(value instanceof ConfigurationSection section)) throw invalid(path, "harus section YAML");
        return section;
    }

    private UUID uuid(String text) {
        UUID id = UUID.fromString(text);
        if (!id.toString().equalsIgnoreCase(text)) throw invalid(text, "UUID tidak valid");
        return id;
    }

    private IllegalArgumentException invalid(String path, String reason) {
        return new IllegalArgumentException(file + ": " + path + " - " + reason);
    }
}