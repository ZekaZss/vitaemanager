package com.bangzachery.vitae.vitaemanager.items;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ItemRulesStore {
    private final Path file, legacyConfig;
    private final ItemCatalog catalog;
    private String base = "";
    private boolean legacy;
    private boolean loaded;

    public ItemRulesStore(Path file, Path legacyConfig, ItemCatalog catalog) {
        this.catalog = catalog;
        this.file = file.toAbsolutePath();
        this.legacyConfig = legacyConfig.toAbsolutePath();
    }

    public ItemRules initialize() throws IOException, InvalidConfigurationException {
        boolean absent = Files.notExists(file);
        ItemRules rules = load();
        if (absent) save(rules);
        return rules;
    }

    public ItemRules load() throws IOException, InvalidConfigurationException {
        if (loaded && Files.notExists(file)) throw new IOException("blocked.yml hilang; reload ditolak");
        String text = Files.notExists(file) ? "" : Files.readString(file, StandardCharsets.UTF_8);
        var yaml = parse(text);
        Object version = yaml.get("vitae-rules-version");
        if (version != null && (!(version instanceof Integer number) || number != 1)) {
            throw new IllegalArgumentException(file + ": vitae-rules-version harus integer 1");
        }
        Set<Material> items = new HashSet<>();
        for (String value : list(yaml, "blocked_items")) items.add(catalog.material(value));
        Set<EntityType> mobs = new HashSet<>();
        for (String value : list(yaml, "blocked_mobs")) mobs.add(catalog.mob(value));
        // config.yml is imported only while the existing blocked.yml has no remake version.
        Map<Material, Integer> limits = version == null && Files.exists(legacyConfig)
                ? limits(parse(Files.readString(legacyConfig, StandardCharsets.UTF_8))) : new HashMap<>();
        limits.putAll(limits(yaml));
        ItemRules rules = new ItemRules(items, mobs, limits);
        base = text; // Commit serialization base only after every field validates.
        legacy = version == null;
        loaded = true;
        return rules;
    }

    public void save(ItemRules rules) throws IOException, InvalidConfigurationException {
        catalog.validate(rules);
        var yaml = parse(base);
        yaml.set("vitae-rules-version", 1);
        yaml.set("blocked_items", rules.blockedItems().stream().map(Enum::name).sorted().toList());
        yaml.set("blocked_mobs", rules.blockedMobs().stream().map(Enum::name).sorted().toList());
        yaml.set("stuck_items", null);
        var section = yaml.createSection("stuck_items");
        rules.stackLimits().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> section.set(entry.getKey().name(), entry.getValue()));
        Files.createDirectories(file.getParent());
        Path backup = file.resolveSibling(file.getFileName() + ".before-remake");
        if (legacy && Files.exists(file) && Files.notExists(backup)) {
            // Backup the exact original bytes; never replace an earlier migration backup.
            Files.writeString(backup, base, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        }
        String updated = yaml.saveToString();
        Path temporary = Files.createTempFile(file.getParent(), "item-rules-", ".tmp");
        try {
            ByteBuffer bytes = ByteBuffer.wrap(updated.getBytes(StandardCharsets.UTF_8));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            base = updated;
            legacy = false;
        } finally { Files.deleteIfExists(temporary); }
    }

    private YamlConfiguration parse(String text) throws InvalidConfigurationException {
        var yaml = new YamlConfiguration();
        yaml.loadFromString(text);
        return yaml;
    }

    private List<String> list(YamlConfiguration yaml, String key) {
        Object value = yaml.get(key);
        if (value == null) return List.of();
        if (!(value instanceof List<?> entries) || entries.stream().anyMatch(entry -> !(entry instanceof String))) {
            throw new IllegalArgumentException(file + ": " + key + " harus list teks");
        }
        return entries.stream().map(String.class::cast).toList();
    }

    private Map<Material, Integer> limits(YamlConfiguration yaml) {
        Map<Material, Integer> limits = new HashMap<>();
        Object value = yaml.get("stuck_items");
        if (value == null) return limits;
        if (!(value instanceof ConfigurationSection section)) throw new IllegalArgumentException("stuck_items harus section");
        for (String key : section.getKeys(false)) {
            Material material = catalog.material(key);
            if (!(section.get(key) instanceof Integer limit)) throw new IllegalArgumentException("Batas stack harus integer: " + key);
            catalog.limit(material, limit);
            if (limits.put(material, limit) != null) throw new IllegalArgumentException("Duplicate stack material: " + key);
        }
        return limits;
    }
}