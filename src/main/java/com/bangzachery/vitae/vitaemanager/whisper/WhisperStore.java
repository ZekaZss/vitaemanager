package com.bangzachery.vitae.vitaemanager.whisper;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Strict owned schema, one serial writer; never rewrites malformed or missing previously loaded files. */
public final class WhisperStore {
    private final Path file;
    private final Set<String> vanillaSounds;
    public WhisperStore(Path file, Set<String> vanillaSounds) {
        this.file = file.toAbsolutePath(); this.vanillaSounds = Set.copyOf(vanillaSounds);
    }
    public WhisperState initialize() throws IOException, InvalidConfigurationException {
        if (Files.notExists(file)) save(WhisperState.empty());
        return load();
    }
    public void validateAudio(WhisperLine.Audio audio) {
        if (audio != null && audio.key().startsWith("minecraft:") && !vanillaSounds.contains(audio.key()))
            throw new IllegalArgumentException("Sound vanilla tidak tersedia di Paper 1.21.4: " + audio.key());
    }
    public WhisperState load() throws IOException, InvalidConfigurationException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        var yaml = new YamlConfiguration(); yaml.loadFromString(source);
        if (integer(yaml, "state-version") != 1) throw invalid("state-version harus integer 1");
        ConfigurationSection entries = section(yaml, "whispers");
        Map<String, WhisperDefinition> definitions = new HashMap<>();
        Map<String, Map<UUID, Integer>> progress = new HashMap<>();
        for (String id : entries.getKeys(false)) {
            WhisperDefinition.requireId(id);
            ConfigurationSection entry = section(entries, id);
            Object flag = entry.get("enabled");
            if (!(flag instanceof Boolean enabled)) throw invalid(id + ".enabled harus boolean");
            WhisperArea area = null;
            if (entry.isSet("area")) {
                var region = section(entry, "area");
                UUID world = uuid(string(region, "world"));
                List<Integer> min = coordinates(region.get("min")), max = coordinates(region.get("max"));
                area = new WhisperArea(world, min.get(0), min.get(1), min.get(2), max.get(0), max.get(1), max.get(2));
            }
            Set<UUID> targets = new HashSet<>();
            Object targetList = entry.get("targets");
            if (!(targetList instanceof List<?> list)) throw invalid(id + ".targets harus list UUID");
            for (Object item : list) {
                if (!(item instanceof String name) || !targets.add(uuid(name))) throw invalid(id + ": penerima tidak valid/duplikat");
            }
            Object rawLines = entry.get("lines");
            if (!(rawLines instanceof List<?> listLines)) throw invalid(id + ".lines harus list dialog");
            List<WhisperLine> lines = new ArrayList<>();
            for (Object raw : listLines) {
                if (!(raw instanceof Map<?, ?> map)) throw invalid(id + ": dialog harus map");
                var line = new YamlConfiguration();
                for (var value : map.entrySet()) {
                    if (!(value.getKey() instanceof String key)) throw invalid(id + ": key dialog harus teks");
                    if (value.getValue() instanceof Map<?, ?> nested) {
                        var child = line.createSection(key);
                        for (var field : nested.entrySet()) {
                            if (!(field.getKey() instanceof String name)) throw invalid(id + ": key sound harus teks");
                            child.set(name, field.getValue());
                        }
                    } else line.set(key, value.getValue());
                }
                WhisperLine.Audio audio = null;
                if (line.isSet("sound")) {
                    var sound = section(line, "sound");
                    audio = new WhisperLine.Audio(string(sound, "key"), decimal(sound, "volume"), decimal(sound, "pitch"));
                    validateAudio(audio);
                }
                lines.add(new WhisperLine(string(line, "title"), string(line, "subtitle"),
                        integer(line, "duration-ticks"), integer(line, "gap-ticks"), audio));
            }
            definitions.put(id, new WhisperDefinition(id, area, targets, lines, integer(entry, "plays"), enabled));
            var counts = section(entry, "completed");
            Map<UUID, Integer> completed = new HashMap<>();
            for (String player : counts.getKeys(false)) {
                if (completed.put(uuid(player), integer(counts, player)) != null) throw invalid("Progres UUID duplikat");
            }
            progress.put(id, completed);
        }
        return new WhisperState(definitions, progress);
    }

    public void save(WhisperState state) throws IOException {
        var yaml = new YamlConfiguration();
        yaml.options().setHeader(List.of("Vitae Bisikan Area — /vitae bisikan help", "Progres 'completed' adalah jumlah rangkaian selesai per UUID.",
                "Sound minecraft: memakai vanilla; namespace custom memerlukan resource pack pemain."));
        yaml.set("state-version", 1);
        var entries = yaml.createSection("whispers");
        for (String id : state.definitions().keySet().stream().sorted().toList()) {
            var definition = state.require(id);
            var entry = entries.createSection(id);
            entry.set("enabled", definition.enabled()); entry.set("plays", definition.limit());
            if (definition.area() != null) {
                var area = definition.area(); var region = entry.createSection("area");
                region.set("world", area.world().toString());
                region.set("min", List.of(area.minX(), area.minY(), area.minZ()));
                region.set("max", List.of(area.maxX(), area.maxY(), area.maxZ()));
            }
            entry.set("targets", definition.targets().stream().map(UUID::toString).sorted().toList());
            List<Map<String, Object>> lines = new ArrayList<>();
            for (var line : definition.lines()) {
                validateAudio(line.audio());
                var map = new LinkedHashMap<String, Object>();
                map.put("title", line.title()); map.put("subtitle", line.subtitle());
                map.put("duration-ticks", line.durationTicks()); map.put("gap-ticks", line.gapTicks());
                if (line.audio() != null) {
                    var audio = line.audio();
                    map.put("sound", Map.of("key", audio.key(), "volume", audio.volume(), "pitch", audio.pitch()));
                }
                lines.add(map);
            }
            entry.set("lines", lines);
            var counts = entry.createSection("completed");
            state.completed().getOrDefault(id, Map.of()).entrySet().stream()
                    .sorted(Map.Entry.comparingByKey()).forEach(value -> counts.set(value.getKey().toString(), value.getValue()));
        }
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "whispers-", ".tmp");
        try {
            ByteBuffer bytes = ByteBuffer.wrap(yaml.saveToString().getBytes(StandardCharsets.UTF_8));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    private IllegalArgumentException invalid(String message) { return new IllegalArgumentException(file + ": " + message); }
    private ConfigurationSection section(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof ConfigurationSection section)) throw invalid(key + " harus section");
        return section;
    }
    private String string(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof String value)) throw invalid(key + " harus teks");
        return value;
    }
    private int integer(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Integer value)) throw invalid(key + " harus integer");
        return value;
    }
    private float decimal(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Number value)) throw invalid(key + " harus angka");
        return value.floatValue();
    }
    private List<Integer> coordinates(Object value) {
        if (!(value instanceof List<?> list) || list.size() != 3 || list.stream().anyMatch(item -> !(item instanceof Integer)))
            throw invalid("Koordinat harus [x, y, z] berisi tiga integer");
        return list.stream().map(Integer.class::cast).toList();
    }
    private UUID uuid(String text) {
        if (!text.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) throw invalid("UUID tidak valid: " + text);
        return UUID.fromString(text);
    }
}