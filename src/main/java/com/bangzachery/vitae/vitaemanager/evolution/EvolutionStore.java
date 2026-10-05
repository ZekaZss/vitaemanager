package com.bangzachery.vitae.vitaemanager.evolution;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import static com.bangzachery.vitae.vitaemanager.evolution.EvolutionAltar.*;

/**
 * Tidak mengakses player, world, registry, atau ItemStack pada worker.
 */
public final class EvolutionStore {
    private final Path file;
    private final Set<String> vanillaSounds;

    public EvolutionStore(Path file, Set<String> vanillaSounds) {
        this.file = file;
        this.vanillaSounds = Set.copyOf(vanillaSounds);
    }

    public enum Phase {
        PREPARED, ACTIVE, REFUNDING, COMMITTING,
        RETURNED, DELIVERED, DISCARDED;

        public boolean terminal() {
            return this == RETURNED || this == DELIVERED || this == DISCARDED;
        }
    }

    public record Ticket(
            UUID id,
            UUID owner,
            String altar,
            Point origin,
            Phase phase,
            List<String> inputs,
            String result
    ) {
        public Ticket {
            Objects.requireNonNull(id);
            Objects.requireNonNull(owner);
            Objects.requireNonNull(origin);
            Objects.requireNonNull(phase);
            require(altar != null && altar.matches("[a-z0-9_-]{1,32}"),
                    "ID recovery tidak valid.");

            inputs = List.copyOf(inputs);
            require(!inputs.isEmpty() && inputs.size() <= 36,
                    "Input recovery tidak valid.");
            inputs.forEach(input -> encoded(input, false));
            encoded(result, false);
        }

        public Ticket phase(Phase next) {
            return new Ticket(id, owner, altar, origin, next, inputs, result);
        }
    }

    public record State(
            Map<String, EvolutionAltar> altars,
            Map<UUID, Ticket> tickets
    ) {
        public State {
            altars = Map.copyOf(altars);
            tickets = Map.copyOf(tickets);
            require(altars.size() <= 128, "Maksimal 128 altar.");

            Set<Point> points = new HashSet<>();
            altars.forEach((id, a) -> {
                require(id.equals(a.id()), "ID altar tidak sesuai.");
                require(points.add(a.table()),
                        "Crafting table sudah digunakan altar lain.");
            });
            tickets.forEach((owner, t) ->
                    require(owner.equals(t.owner()),
                            "Pemilik recovery tidak sesuai."));
        }

        public State put(EvolutionAltar altar) {
            var copy = new HashMap<>(altars);
            copy.put(altar.id(), altar);
            return new State(copy, tickets);
        }

        public State remove(String id) {
            var copy = new HashMap<>(altars);
            require(copy.remove(id) != null, "Altar tidak ditemukan.");
            return new State(copy, tickets);
        }

        public State prepare(Ticket ticket) {
            Ticket previous = tickets.get(ticket.owner());
            require(previous == null || previous.phase().terminal(),
                    "Ritual sebelumnya belum diselesaikan.");

            var copy = new HashMap<>(tickets);
            copy.put(ticket.owner(), ticket);
            return new State(altars, copy);
        }

        public State phase(UUID owner, UUID id, Phase phase) {
            Ticket old = tickets.get(owner);
            require(old != null && old.id().equals(id),
                    "Ticket ritual telah berubah.");
            require(!old.phase().terminal(),
                    "Ticket ritual sudah ditutup.");
            require(phase != Phase.PREPARED,
                    "Tidak boleh kembali ke PREPARED.");
            require(phase != Phase.ACTIVE || old.phase() == Phase.PREPARED,
                    "Transisi ACTIVE tidak valid.");
            require(phase != Phase.DELIVERED || old.phase() == Phase.COMMITTING,
                    "Hasil harus melalui COMMITTING.");

            var copy = new HashMap<>(tickets);
            copy.put(owner, old.phase(phase));
            return new State(altars, copy);
        }
    }

    public State initialize()
            throws IOException, InvalidConfigurationException {
        if (!Files.exists(file)) {
            State empty = new State(Map.of(), Map.of());
            save(empty);
            return empty;
        }
        return load();
    }

    public State load() throws IOException, InvalidConfigurationException {
        if (!Files.isRegularFile(file)) {
            throw new IOException(
                    "File evolution hilang/tidak valid; reload tidak membuat data kosong.");
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.load(file.toFile());
        return parse(yaml);
    }

    public void validateAudio(Cue cue) {
        require(!cue.sound().startsWith("minecraft:")
                        || vanillaSounds.contains(cue.sound()),
                "Sound vanilla tidak tersedia pada versi server.");
    }

    public State parse(YamlConfiguration yaml) {
        require(integer(yaml, "state-version") == 1,
                "Versi evolution tidak didukung.");

        Map<String, EvolutionAltar> altars = new HashMap<>();
        ConfigurationSection root = section(yaml, "altars");

        for (String id : root.getKeys(false)) {
            ConfigurationSection a = section(root, id);
            ConfigurationSection ingredients = section(a, "ingredients");
            ConfigurationSection lines = section(a, "cues");

            List<Ingredient> materials = new ArrayList<>();
            for (int i = 1; i <= ingredients.getKeys(false).size(); i++) {
                ConfigurationSection entry =
                        section(ingredients, Integer.toString(i));

                materials.add(new Ingredient(
                        text(entry, "item"),
                        integer(entry, "count"),
                        Point.parse(text(entry, "point"))
                ));
            }

            Set<UUID> targets = new HashSet<>();
            for (String target : strings(a, "targets")) {
                require(targets.add(UUID.fromString(target)),
                        "Target duplikat.");
            }

            Map<Stage, Cue> cues = new HashMap<>();
            for (String key : lines.getKeys(false)) {
                ConfigurationSection line = section(lines, key);
                Stage stage = Stage.parse(key);
                require(!cues.containsKey(stage), "Tahap duplikat.");

                Cue cue = new Cue(
                        text(line, "title"),
                        text(line, "subtitle"),
                        text(line, "sound"),
                        bool(line, "private"),
                        (float) number(line, "volume"),
                        (float) number(line, "pitch")
                );
                validateAudio(cue);
                cues.put(stage, cue);
            }

            double duration = number(a, "duration");
            range(duration, 5, 120, "Durasi");

            EvolutionAltar altar = new EvolutionAltar(
                    id,
                    Point.parse(text(a, "table")),
                    text(a, "source"),
                    text(a, "result"),
                    materials,
                    targets,
                    Preset.parse(text(a, "preset")),
                    (int) Math.round(duration * 20),
                    number(a, "scale"),
                    integer(a, "density"),
                    number(a, "sound-radius"),
                    number(a, "damage-radius"),
                    number(a, "damage"),
                    bool(a, "enabled"),
                    cues
            );

            for (Stage stage : Stage.values()) validateAudio(altar.cue(stage));
            altars.put(id, altar);
        }

        Map<UUID, Ticket> tickets = new HashMap<>();
        ConfigurationSection recovery = section(yaml, "recovery");

        for (String key : recovery.getKeys(false)) {
            UUID owner = UUID.fromString(key);
            ConfigurationSection t = section(recovery, key);

            tickets.put(owner, new Ticket(
                    UUID.fromString(text(t, "ticket")),
                    owner,
                    text(t, "altar"),
                    Point.parse(text(t, "origin")),
                    Phase.valueOf(text(t, "phase")),
                    strings(t, "inputs"),
                    text(t, "result")
            ));
        }

        return new State(altars, tickets);
    }

    public YamlConfiguration document(State state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("state-version", 1);
        ConfigurationSection root = yaml.createSection("altars");

        for (EvolutionAltar a : new TreeMap<>(state.altars()).values()) {
            ConfigurationSection entry = root.createSection(a.id());
            entry.set("table", a.table().encode());
            entry.set("source", a.source());
            entry.set("result", a.result());
            entry.set("preset", a.preset().name());
            entry.set("duration", a.ticks() / 20.0);
            entry.set("scale", a.scale());
            entry.set("density", a.density());
            entry.set("sound-radius", a.soundRadius());
            entry.set("damage-radius", a.damageRadius());
            entry.set("damage", a.damage());
            entry.set("enabled", a.enabled());
            entry.set("targets", a.targets().stream()
                    .map(UUID::toString).sorted().toList());

            ConfigurationSection ingredients =
                    entry.createSection("ingredients");
            for (int i = 0; i < a.ingredients().size(); i++) {
                writeIngredient(
                        ingredients.createSection(Integer.toString(i + 1)),
                        a.ingredients().get(i)
                );
            }

            ConfigurationSection cues = entry.createSection("cues");
            a.cues().forEach((stage, cue) -> writeCue(
                    cues.createSection(stage.name().toLowerCase(Locale.ROOT)),
                    cue
            ));
        }

        ConfigurationSection recovery = yaml.createSection("recovery");
        state.tickets().forEach((owner, ticket) -> {
            ConfigurationSection t = recovery.createSection(owner.toString());
            t.set("ticket", ticket.id().toString());
            t.set("altar", ticket.altar());
            t.set("origin", ticket.origin().encode());
            t.set("phase", ticket.phase().name());
            t.set("inputs", ticket.inputs());
            t.set("result", ticket.result());
        });
        return yaml;
    }

    public static void writeIngredient(
            ConfigurationSection section, Ingredient ingredient
    ) {
        section.set("item", ingredient.item());
        section.set("count", ingredient.count());
        section.set("point", ingredient.point().encode());
    }

    public static void writeCue(ConfigurationSection section, Cue cue) {
        section.set("title", cue.title());
        section.set("subtitle", cue.subtitle());
        section.set("sound", cue.sound());
        section.set("private", cue.privateSound());
        section.set("volume", cue.volume());
        section.set("pitch", cue.pitch());
    }

    public void save(State state) throws IOException {
        YamlConfiguration yaml = document(state);
        parse(yaml);
        Files.createDirectories(file.toAbsolutePath().getParent());

        Path temp = Files.createTempFile(
                file.toAbsolutePath().getParent(), "evolution-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(
                    temp, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                ByteBuffer bytes = ByteBuffer.wrap(
                        yaml.saveToString().getBytes(StandardCharsets.UTF_8));
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temp, file,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static ConfigurationSection section(
            ConfigurationSection parent, String key
    ) {
        var found = parent.getConfigurationSection(key);
        require(found != null, "Section tidak valid: " + key);
        return found;
    }

    private static String text(ConfigurationSection section, String key) {
        Object value = section.get(key);
        require(value instanceof String, "Harus teks: " + key);
        return (String) value;
    }

    private static boolean bool(ConfigurationSection section, String key) {
        Object value = section.get(key);
        require(value instanceof Boolean, "Harus true/false: " + key);
        return (Boolean) value;
    }

    private static double number(ConfigurationSection section, String key) {
        Object value = section.get(key);
        require(value instanceof Number, "Harus angka: " + key);
        double number = ((Number) value).doubleValue();
        require(Double.isFinite(number), "Angka tidak valid: " + key);
        return number;
    }

    private static int integer(ConfigurationSection section, String key) {
        double number = number(section, key);
        require(number == Math.rint(number)
                        && number >= Integer.MIN_VALUE
                        && number <= Integer.MAX_VALUE,
                "Harus bilangan bulat: " + key);
        return (int) number;
    }

    private static List<String> strings(
            ConfigurationSection section, String key
    ) {
        Object value = section.get(key);
        require(value instanceof List<?>, "Harus daftar: " + key);
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) {
            require(item instanceof String, "Isi daftar harus teks: " + key);
            result.add((String) item);
        }
        return List.copyOf(result);
    }
}