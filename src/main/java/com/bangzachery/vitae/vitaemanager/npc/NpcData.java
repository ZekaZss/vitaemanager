package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.whisper.WhisperArea;
import java.math.BigDecimal;
import java.util.*;

/** Immutable definitions and durable progress. No Bukkit calls in this file. */
public final class NpcData {
    private NpcData() {}
    public enum Kind { HUMAN, MODEL }
    public enum QuestKind { NONE, FARM, MOB }
    public enum GateStatus { CLOSED, OPENING, OPEN, CLOSING }
    public record GateBlock(int x, int y, int z, String data) {
        public GateBlock {
            if (Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000 || Math.abs((long) y) > 4096)
                throw new IllegalArgumentException("Koordinat gerbang di luar batas.");
            data = text(data, 512, "Data block gerbang");
        }
        public String key() { return x + ":" + y + ":" + z; }
    }
    public record GateTiming(int seconds, long closeAt) {
        public GateTiming {
            if (seconds < 0 || seconds > 3600 || closeAt < 0 || (seconds == 0 && closeAt != 0))
                throw new IllegalArgumentException("Tutup otomatis: off atau 1–3600 detik.");
        }
        public static GateTiming defaults() { return new GateTiming(30, 0); }
    }
    public record Gate(WhisperArea area, List<GateBlock> blocks, GateStatus status, GateTiming timing) {
        public Gate {
            Objects.requireNonNull(area); Objects.requireNonNull(status); blocks = List.copyOf(blocks); timing = Objects.requireNonNullElse(timing, GateTiming.defaults());
            if (blocks.isEmpty()) throw new IllegalArgumentException("Seleksi gerbang harus memuat minimal satu blok dinding.");
            Set<String> positions = new HashSet<>();
            for (GateBlock block : blocks) if (!positions.add(block.key()) || !area.contains(area.world(), block.x() + .5, block.y() + .5, block.z() + .5))
                throw new IllegalArgumentException("Block gerbang duplikat atau di luar seleksi.");
        }
        public Gate(WhisperArea area, List<GateBlock> blocks, GateStatus status) { this(area, blocks, status, GateTiming.defaults()); }
        public Gate withStatus(GateStatus next) { return new Gate(area, blocks, next,
                new GateTiming(timing.seconds(), next == GateStatus.OPEN ? timing.closeAt() : 0)); }
        public Gate opened(long now) { return new Gate(area, blocks, GateStatus.OPEN,
                new GateTiming(timing.seconds(), timing.seconds() == 0 ? 0 : Math.addExact(now, timing.seconds() * 1000L))); }
        public Gate withDelay(int seconds, long now) { return new Gate(area, blocks, status,
                new GateTiming(seconds, status == GateStatus.OPEN && seconds > 0 ? Math.addExact(now, seconds * 1000L) : 0)); }
        public boolean due(long now) { return status == GateStatus.OPEN && timing.seconds() > 0 && timing.closeAt() > 0 && now >= timing.closeAt(); }
        public boolean overlaps(Gate other) {
            WhisperArea b = other.area();
            return area.world().equals(b.world()) && area.minX() <= b.maxX() && b.minX() <= area.maxX()
                    && area.minY() <= b.maxY() && b.minY() <= area.maxY() && area.minZ() <= b.maxZ() && b.minZ() <= area.maxZ();
        }
    }
    public record Facing(float yaw, float pitch) {
        public Facing {
            if (!Float.isFinite(yaw) || Math.abs(yaw) > 360 || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Offset arah: yaw -360 sampai 360, pitch -90 sampai 90.");
        }
        public static Facing zero() { return new Facing(0, 0); }
    }
    public record Point(UUID world, double x, double y, double z, float yaw, float pitch) {
        public Point {
            Objects.requireNonNull(world);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                    || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(y) > 4096
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90)
                throw new IllegalArgumentException("Lokasi NPC tidak valid.");
        }
    }
    public record Reward(BigDecimal viti, List<String> items) {
        public Reward {
            viti = VitiAmount.checked(viti);
            items = List.copyOf(items);
            if (items.size() > 27 || (viti.signum() > 0 && !items.isEmpty()))
                throw new IllegalArgumentException("Pilih hadiah Viti atau item, bukan keduanya.");
            for (String item : items) if (item.isBlank() || item.length() > 2_000_000)
                throw new IllegalArgumentException("Data item terlalu besar atau kosong.");
        }
        public static Reward none() { return new Reward(BigDecimal.ZERO, List.of()); }
        public boolean empty() { return viti.signum() == 0 && items.isEmpty(); }
    }
    public record Choice(String label, String target, String memoryKey, String memoryValue) {
        public Choice {
            label = text(label, 80, "Label pilihan");
            target = id(target);
            memoryKey = Objects.requireNonNullElse(memoryKey, ""); memoryValue = Objects.requireNonNullElse(memoryValue, "");
            if (!memoryKey.isEmpty()) { id(memoryKey); id(memoryValue); }
            else if (!memoryValue.isEmpty()) throw new IllegalArgumentException("Kunci ingatan belum diisi.");
        }
        public Choice(String label, String target) { this(label, target, "", ""); }
    }
    public record MemoryRule(String key, String value, String page) {
        public MemoryRule { key = id(key); value = id(value); page = id(page); }
    }
    public record Appearance(boolean nametag, boolean track) {
        public static Appearance defaults() { return new Appearance(true, true); }
    }
    public record Node(String text, List<Choice> choices) {
        public Node { text = NpcData.text(text, 512, "Dialog"); choices = List.copyOf(choices);
            if (choices.size() > 8) throw new IllegalArgumentException("Maksimum delapan pilihan per halaman."); }
        public List<Choice> available() {
            return choices.isEmpty() ? List.of(new Choice("Keluar...", "END")) : choices;
        }
    }
    public record Crop(int x, int y, int z, String plant, String soil) {
        public Crop { plant = text(plant, 200, "Tanaman"); soil = text(soil, 200, "Tanah"); }
        public String key() { return x + ":" + y + ":" + z; }
    }
    public record Quest(QuestKind kind, WhisperArea area, Point start, int goal,
                        String mob, List<Crop> crops, Reward reward) {
        public Quest {
            Objects.requireNonNull(kind); Objects.requireNonNull(reward);
            crops = List.copyOf(crops);
            mob = text(mob, 64, "Jenis mob");
            if (goal < 1 || goal > 50) throw new IllegalArgumentException("Target quest harus 1 sampai 50.");
            if (kind != QuestKind.NONE) {
                Objects.requireNonNull(area); Objects.requireNonNull(start);
                if (!inside(area, start)) throw new IllegalArgumentException("Posisi awal harus di dalam area quest.");
            }
            if (kind == QuestKind.FARM) {
                if (crops.isEmpty() || !reward.items().isEmpty())
                    throw new IllegalArgumentException("Ladang perlu minimal satu tanaman dan hadiah berupa Viti.");
                Set<String> keys = new HashSet<>();
                for (Crop crop : crops) if (!keys.add(crop.key())
                        || !area.contains(area.world(), crop.x() + .5, crop.y() + .5, crop.z() + .5))
                    throw new IllegalArgumentException("Tanaman duplikat atau di luar seleksi.");
            }
        }
        public static Quest none() { return new Quest(QuestKind.NONE, null, null, 1, "COW", List.of(), Reward.none()); }
    }
    public record Definition(String id, UUID token, String name, Kind kind, String model,
                             String texture, String signature, Point location, Point stand,
                             double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                             Reward gift, Quest quest, String repeat, int speed, boolean enabled, Facing facing, Gate gate, Appearance appearance, List<MemoryRule> memory) {
        public Definition {
            facing = Objects.requireNonNullElse(facing, Facing.zero());
            appearance = Objects.requireNonNullElse(appearance, Appearance.defaults());
            memory = memory == null ? List.of() : List.copyOf(memory);
            if (memory.size() > 64) throw new IllegalArgumentException("Maksimal 64 aturan ingatan.");
            id = NpcData.id(id); Objects.requireNonNull(token); name = text(name, 64, "Nama NPC");
            Objects.requireNonNull(kind); Objects.requireNonNull(location); Objects.requireNonNull(stand);
            Objects.requireNonNull(gift); Objects.requireNonNull(quest);
            model = Objects.requireNonNull(model); texture = Objects.requireNonNull(texture);
            signature = Objects.requireNonNull(signature); root = NpcData.id(root);
            nodes = Map.copyOf(nodes); repeat = text(repeat, 512, "Dialog pengulangan");
            if (model.length() > 128 || texture.length() > 16_384 || signature.length() > 16_384)
                throw new IllegalArgumentException("Model atau data skin terlalu besar.");
            if (nodes.isEmpty() || nodes.size() > 128 || !nodes.containsKey(root))
                throw new IllegalArgumentException("Halaman awal tidak ditemukan; maksimum 128 halaman.");
            for (String key : nodes.keySet()) NpcData.id(key);
            if (!Double.isFinite(eyeHeight) || eyeHeight < .2 || eyeHeight > 8 || speed < 1 || speed > 8)
                throw new IllegalArgumentException("Tinggi kepala 0,2–8; kecepatan teks 1–8.");
            if (!location.world().equals(stand.world()) || (trigger != null && !trigger.world().equals(location.world())))
                throw new IllegalArgumentException("NPC, posisi berdiri, dan pemicu harus di dunia yang sama.");
            if (gate != null && !gate.area().world().equals(location.world())) throw new IllegalArgumentException("Gerbang dan NPC harus di dunia yang sama.");
            if (gate != null && gate.area().contains(stand.world(), stand.x(), stand.y(), stand.z()))
                throw new IllegalArgumentException("Posisi bicara harus berada di luar dinding gerbang.");
            Set<String> rules = new HashSet<>();
            for (MemoryRule rule : memory) {
                if (!rules.add(rule.key() + ":" + rule.value())) throw new IllegalArgumentException("Aturan ingatan duplikat.");
                if (enabled && !nodes.containsKey(rule.page())) throw new IllegalArgumentException("Halaman ingatan belum ada: " + rule.page());
            }
            if (enabled) validate(nodes, root, kind, model, quest, gate);
        }
        public Definition(String id, UUID token, String name, Kind kind, String model, String texture, String signature,
                          Point location, Point stand, double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                          Reward gift, Quest quest, String repeat, int speed, boolean enabled, Facing facing, Gate gate) {
            this(id, token, name, kind, model, texture, signature, location, stand, eyeHeight, trigger, root,
                    nodes, gift, quest, repeat, speed, enabled, facing, gate, Appearance.defaults(), List.of());
        }
        /** Retains source compatibility with existing NPC setup/tests. */
        public Definition(String id, UUID token, String name, Kind kind, String model, String texture, String signature,
                          Point location, Point stand, double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                          Reward gift, Quest quest, String repeat, int speed, boolean enabled, Facing facing) {
            this(id, token, name, kind, model, texture, signature, location, stand, eyeHeight, trigger, root,
                    nodes, gift, quest, repeat, speed, enabled, facing, null);
        }
        public Definition(String id, UUID token, String name, Kind kind, String model, String texture, String signature,
                          Point location, Point stand, double eyeHeight, WhisperArea trigger, String root, Map<String, Node> nodes,
                          Reward gift, Quest quest, String repeat, int speed, boolean enabled) {
            this(id, token, name, kind, model, texture, signature, location, stand, eyeHeight, trigger, root,
                    nodes, gift, quest, repeat, speed, enabled, Facing.zero());
        }
        public Definition withGate(Gate next) {
            return new Definition(id, token, name, kind, model, texture, signature, location, stand,
                    eyeHeight, trigger, root, nodes, gift, quest, repeat, speed, enabled, facing, next, appearance, memory);
        }
        public Point facingLocation() {
            float yaw = ((location.yaw() + facing.yaw()) % 360 + 540) % 360 - 180;
            float pitch = Math.max(-90, Math.min(90, location.pitch() + facing.pitch()));
            return new Point(location.world(), location.x(), location.y(), location.z(), yaw, pitch);
        }
        public static Definition create(String id, String name, Point location, Point stand) {
            return new Definition(id, UUID.randomUUID(), name, Kind.HUMAN, "", "", "", location, stand,
                    1.62, null, "start", Map.of("start", new Node("Halo, ada yang bisa kubantu?", List.of())),
                    Reward.none(), Quest.none(), "Aku sudah memberikan hadiah kepadamu. Pergi sana, kamu mencari bansos lagi?", 1, false);
        }
    }
    private static void validate(Map<String, Node> nodes, String root, Kind kind, String model, Quest quest, Gate gate) {
        if (kind == Kind.MODEL && model.isBlank()) throw new IllegalArgumentException("ID model belum diatur.");
        for (Node node : nodes.values()) for (Choice c : node.available()) {
            if (c.target().equals("QUEST")) {
                if (quest.kind() == QuestKind.NONE || quest.reward().empty())
                    throw new IllegalArgumentException("Pilihan QUEST memerlukan quest dan hadiah.");
            } else if (c.target().equals("OPEN")) {
                if (gate == null) throw new IllegalArgumentException("Pilihan OPEN memerlukan gerbang; /vnpc <id> gate set.");
            } else if (!c.target().equals("END") && !com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent.target(c.target()) && !nodes.containsKey(c.target()))
                throw new IllegalArgumentException("Tujuan pilihan tidak ditemukan: " + c.target());
        }
        Set<String> exits = new HashSet<>();
        boolean changed;
        do {
            changed = false;
            for (var entry : nodes.entrySet()) if (!exits.contains(entry.getKey())
                    && entry.getValue().available().stream().anyMatch(c -> c.target().equals("END") || c.target().equals("OPEN")
                    || c.target().equals("QUEST") || com.bangzachery.vitae.vitaemanager.story.StoryNpcActionEvent.target(c.target()) || exits.contains(c.target()))) changed |= exits.add(entry.getKey());
        } while (changed);
        if (exits.size() != nodes.size() || !exits.contains(root))
            throw new IllegalArgumentException("Setiap halaman harus memiliki jalan menuju END, QUEST, atau OPEN.");
    }
    public record Quota(int count, long until) {
        public Quota {
            if (count < 0 || count > 3 || until < 0 || (count < 3 && until != 0) || (count == 3 && until == 0))
                throw new IllegalArgumentException("Data cooldown tidak valid.");
        }
        public Quota effective(long now) { return count == 3 && now >= until ? new Quota(0, 0) : this; }
        public Quota complete(long now) {
            Quota old = effective(now);
            if (old.count == 3) throw new IllegalStateException("Masih cooldown.");
            return old.count == 2 ? new Quota(3, Math.addExact(now, 1_200_000)) : new Quota(old.count + 1, 0);
        }
    }
    public record Award(UUID receipt, UUID owner, String npcName, Reward reward) {
        public Award { Objects.requireNonNull(receipt); Objects.requireNonNull(owner);
            npcName = text(npcName, 64, "Nama NPC"); Objects.requireNonNull(reward); }
    }
    public record State(int version, Map<String, Definition> npcs, Map<UUID, Set<UUID>> completed,
                        Map<UUID, Quota> quotas, Map<UUID, Award> pending, Map<UUID, Map<UUID, Map<String, String>>> memories) {
        public State {
            if (version != 1 || npcs.size() > 128) throw new IllegalArgumentException("Versi data atau jumlah NPC tidak valid.");
            npcs = Map.copyOf(npcs); quotas = Map.copyOf(quotas); pending = Map.copyOf(pending);
            Map<UUID, Map<UUID, Map<String, String>>> frozen = new HashMap<>();
            if (memories != null) memories.forEach((player, byNpc) -> {
                Map<UUID, Map<String, String>> copy = new HashMap<>();
                byNpc.forEach((token, values) -> {
                    if (values.size() > 64) throw new IllegalArgumentException("Maksimal 64 kunci ingatan per NPC/pemain.");
                    values.forEach((key, value) -> { id(key); id(value); }); copy.put(Objects.requireNonNull(token), Map.copyOf(values));
                }); frozen.put(Objects.requireNonNull(player), Map.copyOf(copy));
            }); memories = Map.copyOf(frozen);
            var progress = new HashMap<UUID, Set<UUID>>();
            completed.forEach((player, tokens) -> progress.put(Objects.requireNonNull(player), Set.copyOf(tokens)));
            completed = Map.copyOf(progress);
            npcs.forEach((key, value) -> { if (!key.equals(value.id())) throw new IllegalArgumentException("ID NPC berbeda."); });
            var gated = npcs.values().stream().filter(d -> d.gate() != null).toList();
            for (int i = 0; i < gated.size(); i++) for (int j = i + 1; j < gated.size(); j++)
                if (gated.get(i).gate().overlaps(gated.get(j).gate())) throw new IllegalArgumentException("Seleksi gerbang tidak boleh bertumpuk antar NPC.");
            for (var d : gated) for (var q : npcs.values()) if (q.quest().kind() != QuestKind.NONE && overlaps(d.gate().area(), q.quest().area()))
                throw new IllegalArgumentException("Gerbang tidak boleh bertumpuk dengan area quest.");
            pending.forEach((key, value) -> { if (!key.equals(value.owner())) throw new IllegalArgumentException("Pemilik hadiah berbeda."); });
        }
        public State(int version, Map<String, Definition> npcs, Map<UUID, Set<UUID>> completed,
                     Map<UUID, Quota> quotas, Map<UUID, Award> pending) { this(version, npcs, completed, quotas, pending, Map.of()); }
        public Map<String, String> memory(UUID player, Definition npc) { return memories.getOrDefault(player, Map.of()).getOrDefault(npc.token(), Map.of()); }
        public String recalledRoot(UUID player, Definition npc) {
            Map<String, String> values = memory(player, npc);
            for (MemoryRule rule : npc.memory()) if (rule.value().equals(values.get(rule.key()))) return rule.page();
            return npc.root();
        }
        public boolean recalls(UUID player, Definition npc) {
            Map<String, String> values = memory(player, npc);
            return npc.memory().stream().anyMatch(rule -> rule.value().equals(values.get(rule.key())));
        }
        public State remember(UUID player, Definition npc, Choice choice) {
            if (choice.memoryKey().isEmpty()) return this;
            var all = new HashMap<>(memories); var byNpc = new HashMap<>(all.getOrDefault(player, Map.of()));
            var values = new HashMap<>(byNpc.getOrDefault(npc.token(), Map.of())); values.put(choice.memoryKey(), choice.memoryValue());
            byNpc.put(npc.token(), values); all.put(player, byNpc);
            return new State(1, npcs, completed, quotas, pending, all);
        }
        public static State empty() { return new State(1, Map.of(), Map.of(), Map.of(), Map.of()); }
        public Quota quota(UUID player, long now) { return quotas.getOrDefault(player, new Quota(0, 0)).effective(now); }
        public boolean done(UUID player, Definition npc) { return completed.getOrDefault(player, Set.of()).contains(npc.token()); }
        public State put(Definition d) { var copy = new HashMap<>(npcs); copy.put(d.id(), d); return new State(1, copy, completed, quotas, pending, memories); }
        public State remove(String id) { var copy = new HashMap<>(npcs); copy.remove(id); return new State(1, copy, completed, quotas, pending, memories); }
        public State heard(UUID player, Definition npc) {
            var copy = new HashMap<>(completed); var tokens = new HashSet<>(copy.getOrDefault(player, Set.of()));
            tokens.add(npc.token()); copy.put(player, tokens); return new State(1, npcs, copy, quotas, pending, memories);
        }
        public State finish(UUID player, Definition npc, boolean quest, UUID receipt, long now) {
            if (pending.containsKey(player)) throw new IllegalStateException("Hadiah sebelumnya belum dikirim.");
            if (!quest && done(player, npc)) return this;
            State heard = heard(player, npc);
            var q = new HashMap<>(quotas); if (quest) q.put(player, quota(player, now).complete(now));
            var awards = new HashMap<>(pending);
            Reward reward = quest ? npc.quest().reward() : npc.gift();
            if (!reward.empty()) awards.put(player, new Award(receipt, player, npc.name(), reward));
            return new State(1, npcs, heard.completed, q, awards, memories);
        }
        public State acknowledge(UUID player, UUID receipt) {
            Award a = pending.get(player); if (a == null || !a.receipt().equals(receipt)) return this;
            var copy = new HashMap<>(pending); copy.remove(player); return new State(1, npcs, completed, quotas, copy, memories);
        }
        public State reset(UUID player, Definition npc) {
            if (pending.containsKey(player)) throw new IllegalStateException("Tunggu hadiah terkirim sebelum reset.");
            var c = new HashMap<>(completed); var tokens = new HashSet<>(c.getOrDefault(player, Set.of()));
            tokens.remove(npc.token()); c.put(player, tokens);
            var q = new HashMap<>(quotas); q.remove(player);
            var all = new HashMap<>(memories); var byNpc = new HashMap<>(all.getOrDefault(player, Map.of()));
            byNpc.remove(npc.token()); if (byNpc.isEmpty()) all.remove(player); else all.put(player, byNpc);
            return new State(1, npcs, c, q, pending, all);
        }
    }
    public static String id(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]{1,48}")) throw new IllegalArgumentException("ID harus 1–48 huruf, angka, _ atau -.");
        return value;
    }
    public static String text(String value, int limit, String field) {
        if (value == null || value.isBlank() || value.length() > limit) throw new IllegalArgumentException(field + " kosong atau terlalu panjang.");
        return value;
    }
    public static boolean inside(WhisperArea a, Point p) {
        return a.world().equals(p.world()) && p.x() >= a.minX() && p.x() < a.maxX() + 1.0
                && p.z() >= a.minZ() && p.z() < a.maxZ() + 1.0 && p.y() >= a.minY() - 1 && p.y() < a.maxY() + 3.0;
    }
    public static boolean overlaps(WhisperArea a, WhisperArea b) {
        return a.world().equals(b.world()) && a.minX() <= b.maxX() && b.minX() <= a.maxX()
                && a.minZ() <= b.maxZ() && b.minZ() <= a.maxZ() && a.minY() - 1 <= b.maxY() + 3 && b.minY() - 1 <= a.maxY() + 3;
    }
}
