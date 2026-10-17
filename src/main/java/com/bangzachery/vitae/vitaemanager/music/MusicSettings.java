package com.bangzachery.vitae.vitaemanager.music;

import org.bukkit.configuration.ConfigurationSection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public record MusicSettings(boolean enabled, boolean requirePack, float volume,
                            double crossfadeSeconds, String disabledMessage, List<Track> tracks) {
    public MusicSettings { tracks = List.copyOf(tracks); }
    public record Track(String id, String name, String sound, double seconds) {
        public long durationNanos() { return (long) Math.ceil(seconds * 1_000_000_000d); }
    }
    public static MusicSettings parse(ConfigurationSection source) {
        boolean enabled = bool(source, "enabled");
        boolean requirePack = bool(source, "require-resource-pack");
        double volume = number(source, "volume");
        double crossfade = number(source, "crossfade-seconds");
        if (volume <= 0 || volume > 1) throw invalid("volume harus lebih dari 0 sampai 1");
        if (crossfade < 0 || crossfade * 1_000_000_000d >= Long.MAX_VALUE / 4d)
            throw invalid("crossfade-seconds harus angka nonnegatif yang valid");
        Object messageValue = source.get("admin-off-message");
        if (!(messageValue instanceof String message) || message.isBlank())
            throw invalid("admin-off-message harus berupa teks");
        ConfigurationSection entries = source.getConfigurationSection("tracks");
        if (entries == null || entries.getKeys(false).isEmpty()) throw invalid("tracks belum diisi");
        List<Track> tracks = new ArrayList<>();
        HashSet<String> ids = new HashSet<>();
        for (String rawId : entries.getKeys(false)) {
            String id = rawId.toLowerCase(Locale.ROOT);
            if (!rawId.equals(id) || !id.matches("[a-z0-9_-]+") || !ids.add(id))
                throw invalid("ID lagu tidak valid: " + rawId);
            ConfigurationSection entry = entries.getConfigurationSection(rawId);
            if (entry == null) throw invalid("tracks." + rawId + " harus berupa section");
            Object nameValue = entry.get("name"), soundValue = entry.get("sound");
            if (!(nameValue instanceof String name) || name.isBlank()) throw invalid("nama lagu kosong: " + id);
            if (!(soundValue instanceof String sound) || !sound.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
                throw invalid("sound harus berupa namespace:id untuk " + id);
            double seconds = number(entry, "duration-seconds");
            if (seconds < .05 || seconds * 1_000_000_000d >= Long.MAX_VALUE / 4d)
                throw invalid("duration-seconds tidak valid untuk " + id);
            tracks.add(new Track(id, name, sound, seconds));
        }
        return new MusicSettings(enabled, requirePack, (float) volume, crossfade, message, tracks);
    }
    public int indexOf(String id) {
        for (int i = 0; i < tracks.size(); i++) if (tracks.get(i).id().equalsIgnoreCase(id)) return i;
        return -1;
    }
    private static boolean bool(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Boolean result)) throw invalid(key + " harus true/false");
        return result;
    }
    private static double number(ConfigurationSection source, String key) {
        if (!(source.get(key) instanceof Number number) || !Double.isFinite(number.doubleValue()))
            throw invalid(key + " harus angka yang valid");
        return number.doubleValue();
    }
    private static IllegalArgumentException invalid(String detail) {
        return new IllegalArgumentException("music.yml: " + detail);
    }
}
