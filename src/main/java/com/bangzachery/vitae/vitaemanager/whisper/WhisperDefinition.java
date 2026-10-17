package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.*;

public record WhisperDefinition(String id, WhisperArea area, Set<UUID> targets, List<WhisperLine> lines,
                                int limit, boolean enabled, WhisperMusic music, WhisperVisual visual) {
    public WhisperDefinition {
        requireId(id);
        targets = Set.copyOf(targets);
        lines = List.copyOf(lines);
        visual = Objects.requireNonNullElse(visual, WhisperVisual.defaults());
        if (targets.size() > 1000 || lines.size() > 100 || limit < 1 || limit > 100)
            throw new IllegalArgumentException("Maksimum 1000 penerima, 100 dialog dan batas ulang 1–100.");
        if (enabled && (area == null || targets.isEmpty() || lines.isEmpty()))
            throw new IllegalArgumentException("Lengkapi area, penerima dan dialog sebelum enable.");
    }
    public WhisperDefinition(String id, WhisperArea area, Set<UUID> targets, List<WhisperLine> lines, int limit, boolean enabled) {
        this(id, area, targets, lines, limit, enabled, null, WhisperVisual.defaults());
    }

    public static void requireId(String id) {
        if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,47}"))
            throw new IllegalArgumentException("ID harus 1–48 huruf kecil/angka/underscore/tanda minus.");
    }
    public static WhisperDefinition draft(String id) { return new WhisperDefinition(id, null, Set.of(), List.of(), 1, false); }
    public WhisperDefinition withArea(WhisperArea value) { return new WhisperDefinition(id, value, targets, lines, limit, enabled, music, visual); }
    public WhisperDefinition withTargets(Set<UUID> value) { return new WhisperDefinition(id, area, value, lines, limit, enabled, music, visual); }
    public WhisperDefinition withLines(List<WhisperLine> value) { return new WhisperDefinition(id, area, targets, value, limit, enabled, music, visual); }
    public WhisperDefinition withLimit(int value) { return new WhisperDefinition(id, area, targets, lines, value, enabled, music, visual); }
    public WhisperDefinition withEnabled(boolean value) { return new WhisperDefinition(id, area, targets, lines, limit, value, music, visual); }
    public WhisperDefinition withMusic(WhisperMusic value) { return new WhisperDefinition(id, area, targets, lines, limit, enabled, value, visual); }
    public WhisperDefinition withVisual(WhisperVisual value) { return new WhisperDefinition(id, area, targets, lines, limit, enabled, music, value); }
    public long durationTicks() {
        long total = 0;
        for (int i = 0; i < lines.size(); i++) total += lines.get(i).durationTicks() + (i + 1 < lines.size() ? lines.get(i).gapTicks() : 0);
        return total;
    }
}
