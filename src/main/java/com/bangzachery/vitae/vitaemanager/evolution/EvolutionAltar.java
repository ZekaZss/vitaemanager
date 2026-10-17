package com.bangzachery.vitae.vitaemanager.evolution;

import java.util.*;

public record EvolutionAltar(
        String id,
        Point table,
        String source,
        String result,
        List<Ingredient> ingredients,
        Set<UUID> targets,
        Preset preset,
        int ticks,
        double scale,
        int density,
        double soundRadius,
        double damageRadius,
        double damage,
        boolean enabled,
        Map<Stage, Cue> cues, Music music, Effects effects, boolean lethal, Aftermath aftermath
) {
    public EvolutionAltar {
        require(id != null && id.matches("[a-z0-9_-]{1,32}"),
                "ID: 1–32 huruf kecil, angka, _ atau -.");

        Objects.requireNonNull(table);
        Objects.requireNonNull(preset);
        Objects.requireNonNull(music); Objects.requireNonNull(effects); aftermath = Objects.requireNonNullElse(aftermath, Aftermath.defaults());
        encoded(source, true);
        encoded(result, true);

        List<Ingredient> positioned = new ArrayList<>();
        Set<Integer> slots = new HashSet<>();
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            int slot = ingredient.slot() == 0 ? EvolutionGrid.rawSlot(i) : ingredient.slot();
            require(slots.add(slot), "Slot bahan tidak boleh duplikat.");
            positioned.add(new Ingredient(ingredient.item(), ingredient.count(), ingredient.point(), slot));
        }
        ingredients = List.copyOf(positioned);
        targets = Set.copyOf(targets);
        cues = Map.copyOf(cues);

        require(ingredients.size() <= 8 && targets.size() <= 256,
                "Maksimal 8 bahan dan 256 penerima.");
        require(ticks >= 100 && ticks <= 2400,
                "Durasi harus 5–120 detik.");
        range(scale, .5, 2, "Scale");
        require(density >= 8 && density <= 64, "Density harus 8–64.");
        range(soundRadius, 1, 64, "Radius suara");
        range(damageRadius, 0, 16, "Radius damage");
        range(damage, 0, 1000, "Damage");
        require(music.sound().isEmpty() || music.ticks() == ticks, "Durasi musik harus sama dengan ritual.");

        Set<Point> places = new HashSet<>();
        for (Ingredient ingredient : ingredients) {
            if (ingredient.point() == null) continue;
            require(ingredient.point().distanceSquared(table) <= 256
                            && !ingredient.point().equals(table),
                    "Pedestal harus berbeda dari altar dan maksimal 16 block di world yang sama.");
            require(places.add(ingredient.point()),
                    "Pedestal bahan tidak boleh duplikat.");
        }

        // Validasi parameter: field record belum diisi pada compact constructor.
        require(!enabled || (!source.isEmpty() && !result.isEmpty()
                        && !ingredients.isEmpty() && !targets.isEmpty()
                        && ingredients.stream().allMatch(i -> i.point() != null)),
                "Lengkapi source, result, bahan, pedestal tiap slot, dan target sebelum enable.");
    }

    public record Aftermath(boolean enabled, int ticks) {
        public Aftermath { require(ticks >= 40 && ticks <= 300, "Adegan penutup harus 2–15 detik."); }
        public static Aftermath defaults() { return new Aftermath(true, 100); }
        public int quietTicks() { return 20; }
    }
    public EvolutionAltar(String id, Point table, String source, String result,
            List<Ingredient> ingredients, Set<UUID> targets, Preset preset,
            int ticks, double scale, int density, double soundRadius,
            double damageRadius, double damage, boolean enabled, Map<Stage, Cue> cues, Music music, Effects effects, boolean lethal) {
        this(id, table, source, result, ingredients, targets, preset, ticks, scale, density, soundRadius, damageRadius,
                damage, enabled, cues, music, effects, lethal, Aftermath.defaults());
    }
    public EvolutionAltar(String id, Point table, String source, String result,
            List<Ingredient> ingredients, Set<UUID> targets, Preset preset,
            int ticks, double scale, int density, double soundRadius,
            double damageRadius, double damage, boolean enabled, Map<Stage, Cue> cues) {
        this(id, table, source, result, ingredients, targets, preset, ticks, scale,
                density, soundRadius, damageRadius, damage, enabled, cues,
                Music.off(ticks), Effects.off(), false);
    }

    public record Music(String sound, boolean privateSound, float volume, int ticks) {
        public Music {
            require(sound != null && (sound.isEmpty() || sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")),
                    "Musik: namespace:nama atau off.");
            range(volume, 0, 1, "Volume musik");
            require(ticks >= 100 && ticks <= 2400, "Durasi musik 5–120 detik.");
        }
        public static Music off(int ticks) { return new Music("", false, .8f, ticks); }
    }
    public record Effects(boolean lightning, int interval, double radius, boolean shockwave) {
        public Effects {
            require(interval >= 10 && interval <= 200, "Jeda petir 0.5–10 detik.");
            range(radius, 0, 8, "Radius petir");
        }
        public static Effects off() { return new Effects(false, 30, 3, true); }
    }

    public static EvolutionAltar draft(String id, Point point) {
        return new EvolutionAltar(
                id, point, "", "", List.of(), Set.of(),
                Preset.INFERNO, 300, 1, 24, 24, 9, 0, false, Map.of(), Music.off(300), Effects.off(), false
        );
    }

    public boolean complete() {
        return !source.isEmpty() && !result.isEmpty()
                && !ingredients.isEmpty() && !targets.isEmpty()
                && ingredients.stream().allMatch(i -> i.point() != null);
    }

    public enum Preset {
        INFERNO, STORMFORGE, VOID, ASTRAL, FROST, PHOENIX;

        public static Preset parse(String text) {
            try {
                return valueOf(text.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Preset: inferno, stormforge, void, astral, frost, phoenix.");
            }
        }
    }

    public enum Stage {
        AWAKEN, LIFT, ORBIT, ABSORB, FINISH;

        public static Stage parse(String text) {
            try {
                return valueOf(text.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                        "Tahap: awaken, lift, orbit, absorb, finish.");
            }
        }
    }

    public record Point(UUID world, int x, int y, int z) {
        public Point {
            Objects.requireNonNull(world);
            require(Math.abs((long) x) <= 30_000_000
                            && Math.abs((long) z) <= 30_000_000
                            && y >= -4096 && y <= 4096,
                    "Koordinat di luar batas.");
        }

        public double distanceSquared(Point other) {
            if (!world.equals(other.world)) return Double.POSITIVE_INFINITY;
            double dx = (double) x - other.x;
            double dy = (double) y - other.y;
            double dz = (double) z - other.z;
            return dx * dx + dy * dy + dz * dz;
        }

        public String encode() {
            return world + " " + x + " " + y + " " + z;
        }

        public static Point parse(String text) {
            String[] a = text.split(" ");
            require(a.length == 4, "Koordinat tidak valid.");
            return new Point(
                    UUID.fromString(a[0]),
                    Integer.parseInt(a[1]),
                    Integer.parseInt(a[2]),
                    Integer.parseInt(a[3])
            );
        }
    }

    public record Ingredient(String item, int count, Point point, int slot) {
        public Ingredient {
            encoded(item, false);
            require(slot == 0 || EvolutionGrid.ingredientSlot(slot), "Slot bahan: 1, 2, 3, 4, 6, 7, 8, 9; slot 5 khusus senjata.");
            require(count >= 1 && count <= 64,
                    "Jumlah bahan harus 1–64.");
        }
        public Ingredient(String item, int count, Point point) { this(item, count, point, 0); }
    }

    public record Cue(
            String title,
            String subtitle,
            String sound,
            boolean privateSound,
            float volume,
            float pitch
    ) {
        public Cue {
            require(title != null && subtitle != null
                            && title.length() <= 120 && subtitle.length() <= 240,
                    "Title maksimal 120, subtitle 240 karakter.");
            require(!title.matches("(?s).*[\\r\\n\\p{Cntrl}].*")
                            && !subtitle.matches("(?s).*[\\r\\n\\p{Cntrl}].*"),
                    "Teks harus satu baris.");
            require(sound != null && (sound.isEmpty()
                            || sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")),
                    "Sound: namespace:nama atau off.");
            range(volume, 0, 1, "Volume");
            range(pitch, .5, 2, "Pitch");
        }
    }

    public Cue cue(Stage stage) {
        if (cues.containsKey(stage)) return cues.get(stage);

        String[] sounds = switch (preset) {
            case INFERNO, PHOENIX -> new String[]{"block.fire.ambient", "entity.blaze.ambient",
                    "block.respawn_anchor.charge", "entity.blaze.shoot", "entity.generic.explode"};
            case STORMFORGE -> new String[]{"block.beacon.activate", "block.anvil.use",
                    "block.amethyst_block.resonate", "entity.lightning_bolt.thunder", "entity.generic.explode"};
            case VOID -> new String[]{"block.portal.ambient", "block.enchantment_table.use",
                    "block.respawn_anchor.ambient", "entity.enderman.teleport", "entity.generic.explode"};
            case ASTRAL -> new String[]{"block.beacon.activate", "block.amethyst_block.chime",
                    "block.amethyst_block.resonate", "block.respawn_anchor.charge", "entity.generic.explode"};
            case FROST -> new String[]{"block.glass.place", "block.amethyst_block.chime",
                    "block.beacon.ambient", "block.glass.break", "entity.generic.explode"};
        };

        String[] titles = {
                "Ritual dimulai", "Kekuatan terbangun",
                "Penyatuan", "Evolusi", "Evolusi selesai"
        };
        String[] subtitles = {
                "Bahan telah diterima", "Senjata menjawab panggilan",
                "Energi mengelilingi senjata", "Kekuatan baru terbentuk",
                "Ambil senjatamu"
        };

        return new Cue(
                titles[stage.ordinal()],
                subtitles[stage.ordinal()],
                "minecraft:" + sounds[stage.ordinal()],
                false, .5f, 1
        );
    }

    public static void encoded(String text, boolean emptyAllowed) {
        require(text != null && text.length() <= 180_000,
                "Data item tidak valid atau terlalu besar.");
        if (emptyAllowed && text.isEmpty()) return;
        byte[] bytes = Base64.getDecoder().decode(text);
        require(bytes.length > 0, "Data item kosong.");
    }

    public static void range(
            double value, double min, double max, String name
    ) {
        require(Double.isFinite(value) && value >= min && value <= max,
                name + " harus " + min + "–" + max + ".");
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
