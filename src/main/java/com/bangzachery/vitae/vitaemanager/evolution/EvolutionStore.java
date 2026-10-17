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
        PREPARED, ACTIVE, REFUNDING, COMMITTING, REVEALING,
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

    public record Deposit(UUID id, UUID owner, String altar, Point origin,
                          Map<Integer, String> items, Transfer transfer) {
        public Deposit {
            Objects.requireNonNull(id); Objects.requireNonNull(owner); Objects.requireNonNull(origin);
            require(altar != null && altar.matches("[a-z0-9_-]{1,32}"), "ID setoran tidak valid.");
            items = checkedSlots(items);
        }
        public Deposit pending(Transfer next) {
            require(transfer == null, "Setoran sebelumnya masih diproses.");
            return new Deposit(id, owner, altar, origin, items, next);
        }
        public Deposit settle(boolean consumed) {
            require(transfer != null, "Tidak ada transaksi setoran.");
            return new Deposit(id, owner, altar, origin, consumed ? transfer.next() : items, null);
        }
    }
    /** Journal ditulis sebelum cursor dipotong; receipt disimpan di data pemain. */
    public record Transfer(UUID id, String cursor, Map<Integer, String> next) {
        public Transfer {
            Objects.requireNonNull(id); encoded(cursor, false); next = checkedSlots(next);
            require(!next.isEmpty(), "Setoran kosong.");
        }
    }
    private static Map<Integer, String> checkedSlots(Map<Integer, String> source) {
        var copy = Map.copyOf(source); require(copy.size() <= 9, "Maksimal 9 slot setoran.");
        copy.forEach((slot, item) -> {
            require(slot >= 0 && slot <= 8, "Slot setoran tidak valid."); encoded(item, false);
        });
        return copy;
    }

    public record State(
            Map<String, EvolutionAltar> altars,
            Map<UUID, Ticket> tickets, Map<UUID, Deposit> deposits
    ) {
        public State {
            altars = Map.copyOf(altars);
            tickets = Map.copyOf(tickets);
            deposits = Map.copyOf(deposits);
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
            var definitions = altars; var receipts = tickets;
            Set<String> reserved = new HashSet<>();
            require(deposits.size() <= 128, "Maksimal 128 setoran.");
            deposits.forEach((owner, d) -> {
                require(owner.equals(d.owner()), "Pemilik setoran tidak sesuai.");
                EvolutionAltar a = definitions.get(d.altar());
                require(a != null && a.table().equals(d.origin()), "Altar setoran hilang atau dipindahkan.");
                require(a.ingredients().stream().allMatch(i -> i.point() != null), "Setoran memiliki pedestal yang belum dipetakan.");
                require(reserved.add(d.altar()), "Altar memiliki dua pemilik setoran.");
                Ticket t = receipts.get(owner);
                require(t == null || t.phase().terminal(), "Pemilik punya ritual dan setoran bersamaan.");
            });
        }

        public State(Map<String, EvolutionAltar> altars, Map<UUID, Ticket> tickets) {
            this(altars, tickets, Map.of());
        }
        public State deposit(Deposit next) {
            Deposit old = deposits.get(next.owner());
            require(old == null || old.id().equals(next.id()), "Setoran telah berubah.");
            var copy = new HashMap<>(deposits); copy.put(next.owner(), next);
            return new State(altars, tickets, copy);
        }
        public State removeDeposit(UUID owner, UUID id) {
            Deposit old = deposits.get(owner);
            require(old != null && old.id().equals(id), "Setoran telah berubah.");
            var copy = new HashMap<>(deposits); copy.remove(owner);
            return new State(altars, tickets, copy);
        }
        public State settle(UUID owner, UUID id, UUID receipt, boolean consumed) {
            Deposit old = deposits.get(owner);
            require(old != null && old.id().equals(id) && old.transfer() != null
                    && old.transfer().id().equals(receipt), "Transaksi setoran telah berubah.");
            Deposit next = old.settle(consumed);
            return next.items().isEmpty() ? removeDeposit(owner, id) : deposit(next);
        }
        public State beginDeposit(Ticket ticket, UUID depositId) {
            Deposit old = deposits.get(ticket.owner());
            require(old != null && old.id().equals(depositId) && old.transfer() == null, "Setoran belum siap.");
            require(ticket.inputs().equals(new TreeMap<>(old.items()).values().stream().toList()),
                    "Input ritual tidak sesuai setoran.");
            return removeDeposit(old.owner(), old.id()).prepare(ticket);
        }

        public State put(EvolutionAltar altar) {
            var copy = new HashMap<>(altars);
            copy.put(altar.id(), altar);
            return new State(copy, tickets, deposits);
        }

        public State remove(String id) {
            var copy = new HashMap<>(altars);
            require(copy.remove(id) != null, "Altar tidak ditemukan.");
            return new State(copy, tickets, deposits);
        }

        public State prepare(Ticket ticket) {
            Ticket previous = tickets.get(ticket.owner());
            require(previous == null || previous.phase().terminal(),
                    "Ritual sebelumnya belum diselesaikan.");

            var copy = new HashMap<>(tickets);
            copy.put(ticket.owner(), ticket);
            return new State(altars, copy, deposits);
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
            require(phase != Phase.REVEALING || old.phase() == Phase.COMMITTING, "Adegan hasil harus melalui COMMITTING.");
            require(old.phase() != Phase.REVEALING || phase == Phase.REVEALING || phase == Phase.DELIVERED || phase == Phase.DISCARDED,
                    "Ledakan sudah dipastikan; hanya hasil atau discard, bukan refund.");
            require(phase != Phase.DELIVERED || old.phase() == Phase.COMMITTING || old.phase() == Phase.REVEALING,
                    "Hasil harus melalui COMMITTING.");

            var copy = new HashMap<>(tickets);
            copy.put(owner, old.phase(phase));
            return new State(altars, copy, deposits);
        }
    }

    public State initialize()
            throws IOException, InvalidConfigurationException {
        if (!Files.exists(file)) {
            State empty = new State(Map.of(), Map.of());
            save(empty);
            return empty;
        }
        State loaded = load();
        YamlConfiguration old = new YamlConfiguration(); old.load(file.toFile());
        boolean upgrade = loaded.altars().keySet().stream().anyMatch(id -> !old.contains("altars." + id + ".aftermath"));
        if (upgrade) {
            Files.copy(file, file.resolveSibling("evolution-before-story-update-" + UUID.randomUUID() + ".yml.bak"));
            save(loaded);
        }
        return loaded;
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
        int version = integer(yaml, "state-version");
        require(version == 1 || version == 2,
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
                int slot = entry.contains("slot") ? integer(entry, "slot") : EvolutionGrid.rawSlot(i - 1);
                require(EvolutionGrid.ingredientSlot(slot), "Slot bahan tersimpan tidak valid.");
                materials.add(new Ingredient(
                        text(entry, "item"),
                        integer(entry, "count"),
                        text(entry, "point").isEmpty() ? null : Point.parse(text(entry, "point")),
                        slot
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
            int ticks = (int) Math.round(duration * 20);
            Music music = Music.off(ticks); Effects effects = Effects.off();
            if (version == 2 || a.contains("music")) {
                ConfigurationSection m = section(a, "music");
                music = new Music(text(m, "sound"), bool(m, "private"), (float) number(m, "volume"), ticks);
                validateAudio(new Cue("", "", music.sound(), music.privateSound(), music.volume(), 1));
            }
            if (version == 2 || a.contains("effects")) {
                ConfigurationSection e = section(a, "effects");
                effects = new Effects(bool(e, "lightning"), integer(e, "interval-ticks"),
                        number(e, "radius"), bool(e, "shockwave"));
            }


            Aftermath aftermath = Aftermath.defaults();
            if (a.contains("aftermath")) {
                ConfigurationSection ending = section(a, "aftermath");
                double seconds = number(ending, "duration"); range(seconds, 2, 15, "Adegan penutup");
                aftermath = new Aftermath(bool(ending, "enabled"), (int) Math.round(seconds * 20));
            }
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
                    cues, music, effects, (version == 2 || a.contains("lethal")) && bool(a, "lethal"), aftermath
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

        Map<UUID, Deposit> deposits = new HashMap<>();
        if (version == 2 || yaml.contains("deposits")) {
            ConfigurationSection ds = section(yaml, "deposits");
            for (String key : ds.getKeys(false)) {
                UUID owner = UUID.fromString(key); ConfigurationSection d = section(ds, key);
                Transfer transfer = null;
                if (d.contains("transfer")) {
                    ConfigurationSection t = section(d, "transfer");
                    transfer = new Transfer(UUID.fromString(text(t, "id")), text(t, "cursor"), readSlots(section(t, "next")));
                }
                deposits.put(owner, new Deposit(UUID.fromString(text(d, "id")), owner, text(d, "altar"),
                        Point.parse(text(d, "origin")), readSlots(section(d, "items")), transfer));
            }
        }
        return new State(altars, tickets, deposits);
    }

    public YamlConfiguration document(State state) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("state-version", 2);
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
            entry.set("lethal", a.lethal());
            entry.set("music.sound", a.music().sound());
            entry.set("music.private", a.music().privateSound());
            entry.set("music.volume", a.music().volume());
            entry.set("effects.lightning", a.effects().lightning());
            entry.set("effects.interval-ticks", a.effects().interval());
            entry.set("effects.radius", a.effects().radius());
            entry.set("effects.shockwave", a.effects().shockwave());
            entry.set("aftermath.enabled", a.aftermath().enabled());
            entry.set("aftermath.duration", a.aftermath().ticks() / 20.0);
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
        ConfigurationSection deposits = yaml.createSection("deposits");
        state.deposits().forEach((owner, deposit) -> {
            ConfigurationSection d = deposits.createSection(owner.toString());
            d.set("id", deposit.id().toString()); d.set("altar", deposit.altar()); d.set("origin", deposit.origin().encode());
            writeSlots(d.createSection("items"), deposit.items());
            if (deposit.transfer() != null) {
                Transfer transfer = deposit.transfer(); ConfigurationSection t = d.createSection("transfer");
                t.set("id", transfer.id().toString()); t.set("cursor", transfer.cursor());
                writeSlots(t.createSection("next"), transfer.next());
            }
        });
        return yaml;
    }
    private static Map<Integer, String> readSlots(ConfigurationSection section) {
        Map<Integer, String> slots = new HashMap<>();
        for (String key : section.getKeys(false)) {
            int index = Integer.parseInt(key);
            require(Integer.toString(index).equals(key), "Nomor slot harus kanonik."); slots.put(index, text(section, key));
        }
        return checkedSlots(slots);
    }
    private static void writeSlots(ConfigurationSection section, Map<Integer, String> slots) {
        new TreeMap<>(slots).forEach((slot, item) -> section.set(slot.toString(), item));
    }

    public static void writeIngredient(
            ConfigurationSection section, Ingredient ingredient
    ) {
        section.set("item", ingredient.item());
        section.set("count", ingredient.count());
        section.set("point", ingredient.point() == null ? "" : ingredient.point().encode());
        section.set("slot", ingredient.slot());
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
