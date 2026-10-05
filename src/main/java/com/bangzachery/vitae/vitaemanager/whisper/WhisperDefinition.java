package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.*;

public record WhisperDefinition(String id, WhisperArea area, Set<UUID> targets, List<WhisperLine> lines,
                                int limit, boolean enabled) {
    public WhisperDefinition {
        requireId(id);
        targets = Set.copyOf(targets);
        lines = List.copyOf(lines);
        if (targets.size() > 1000 || lines.size() > 100 || limit < 1 || limit > 100)
            throw new IllegalArgumentException("Maksimum 1000 penerima, 100 dialog dan batas ulang 1–100.");
        if (enabled && (area == null || targets.isEmpty() || lines.isEmpty()))
            throw new IllegalArgumentException("Lengkapi area, penerima dan dialog sebelum enable.");
    }

    public static void requireId(String id) {
        if (id == null || !id.matches("[a-z0-9][a-z0-9_-]{0,47}"))
            throw new IllegalArgumentException("ID harus 1–48 huruf kecil/angka/underscore/tanda minus.");
    }
    public static WhisperDefinition draft(String id) { return new WhisperDefinition(id, null, Set.of(), List.of(), 1, false); }
    public WhisperDefinition withArea(WhisperArea value) { return new WhisperDefinition(id, value, targets, lines, limit, enabled); }
    public WhisperDefinition withTargets(Set<UUID> value) { return new WhisperDefinition(id, area, value, lines, limit, enabled); }
    public WhisperDefinition withLines(List<WhisperLine> value) { return new WhisperDefinition(id, area, targets, value, limit, enabled); }
    public WhisperDefinition withLimit(int value) { return new WhisperDefinition(id, area, targets, lines, value, enabled); }
    public WhisperDefinition withEnabled(boolean value) { return new WhisperDefinition(id, area, targets, lines, limit, value); }
}