package com.bangzachery.vitae.vitaemanager.magic;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.*;

/** Immutable, validated audio parameters; parsing never rewrites administrator configuration. */
final class SpellSoundPlan {
    static final List<String> PHASES = List.of("activation", "formation", "active", "release", "ending");
    enum Audience { OWNER, VICTIMS, PARTICIPANTS, NEARBY }
    enum Origin { CENTER, OWNER, VICTIM }
    record Cue(String type, String id, float volume, float pitch, int intervalTicks,
               Audience audience, Origin origin, double range) {}
    private final Map<String, List<Cue>> phases;
    private SpellSoundPlan(Map<String, List<Cue>> phases) { this.phases = Map.copyOf(phases); }
    List<Cue> cues(String phase) { return phases.getOrDefault(phase, List.of()); }

    static SpellSoundPlan parse(YamlConfiguration config, YamlConfiguration defaults) {
        if (config.isSet("sounds") && !config.isConfigurationSection("sounds")) throw invalid("sounds");
        Map<String, List<Cue>> result = new LinkedHashMap<>();
        for (String phase : PHASES) {
            String path = "sounds." + phase;
            if (config.isSet(path) && !config.isConfigurationSection(path)) throw invalid(path);
            ConfigurationSection own = config.getConfigurationSection(path), fallback = defaults.getConfigurationSection(path);
            if (own == null && fallback == null) continue;
            if (!bool(own, fallback, "enabled", true)) { result.put(phase, List.of()); continue; }
            List<Map<?, ?>> values = new ArrayList<>();
            if (own != null && own.getKeys(false).contains("id")) {
                // Legacy single-cue phase is authoritative, including custom resource-pack IDs.
                values.add(own.getValues(false));
            } else {
                ConfigurationSection source = own != null && own.getKeys(false).contains("cues") ? own : fallback;
                if (source != null && source.contains("cues")) {
                    Object list = source.get("cues");
                    if (!(list instanceof List<?> entries)) throw invalid(path + ".cues");
                    for (Object entry : entries) {
                        if (!(entry instanceof Map<?, ?> map)) throw invalid(path + ".cues");
                        values.add(map);
                    }
                } else if (own != null) values.add(own.getValues(false));
            }
            List<Cue> cues = new ArrayList<>();
            for (Map<?, ?> value : values) {
                String type = text(value, own, fallback, "type", "vanilla");
                String id = text(value, null, null, "id", "minecraft:entity.warden.heartbeat");
                SpellConfig.validateSoundSyntax(type, id);
                int defaultInterval = phase.equals("active") ? 50 : phase.equals("formation") ? 80 : 0;
                double interval = number(value, own, fallback, "interval-ticks", defaultInterval, 0, 72000);
                if (interval != Math.rint(interval)) throw invalid(path + ".interval-ticks");
                // Old configs accepted interval on these burst phases; preserve acceptance but never repeat closure.
                if (phase.equals("activation") || phase.equals("ending")) interval = 0;
                cues.add(new Cue(type, id,
                        (float) number(value, own, fallback, "volume", 1, 0, 16),
                        (float) number(value, own, fallback, "pitch", 1, .01, 2), (int) interval,
                        enumValue(Audience.class, text(value, own, fallback, "audience", "nearby")),
                        enumValue(Origin.class, text(value, own, fallback, "origin", "center")),
                        number(value, own, fallback, "range", 32, 0, 128)));
            }
            result.put(phase, List.copyOf(cues));
        }
        return new SpellSoundPlan(result);
    }

    private static Object field(Map<?, ?> value, ConfigurationSection own, ConfigurationSection defaults, String key, Object def) {
        if (value.containsKey(key)) return value.get(key);
        if (own != null && own.getKeys(false).contains(key)) return own.get(key);
        return defaults == null ? def : defaults.get(key, def);
    }
    private static String text(Map<?, ?> value, ConfigurationSection own, ConfigurationSection defaults, String key, String def) {
        Object result = field(value, own, defaults, key, def);
        if (!(result instanceof String string)) throw invalid(key);
        return string;
    }
    private static double number(Map<?, ?> value, ConfigurationSection own, ConfigurationSection defaults, String key, double def, double min, double max) {
        Object result = field(value, own, defaults, key, def);
        if (!(result instanceof Number number)) throw invalid(key);
        double n = number.doubleValue();
        if (!Double.isFinite(n) || n < min || n > max) throw invalid(key);
        return n;
    }
    private static boolean bool(ConfigurationSection own, ConfigurationSection defaults, String key, boolean def) {
        Object result = field(Map.of(), own, defaults, key, def);
        if (!(result instanceof Boolean bool)) throw invalid(key);
        return bool;
    }
    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        try { return Enum.valueOf(type, value.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw invalid(type.getSimpleName() + ": " + value); }
    }
    private static IllegalArgumentException invalid(String key) { return new IllegalArgumentException("Konfigurasi sound tidak valid: " + key); }
}
