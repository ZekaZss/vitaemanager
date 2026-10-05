package com.bangzachery.vitae.vitaemanager.whisper;

import java.util.*;

/** Definitions and completed sequences share one atomic file, so reset/edit cannot lose receipts. */
public record WhisperState(Map<String, WhisperDefinition> definitions, Map<String, Map<UUID, Integer>> completed) {
    public WhisperState {
        definitions = Map.copyOf(definitions);
        if (definitions.size() > 500) throw new IllegalArgumentException("Maksimum 500 area bisikan.");
        for (var entry : definitions.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().id())) throw new IllegalArgumentException("ID area tidak sesuai key.");
        }
        var copy = new HashMap<String, Map<UUID, Integer>>();
        for (var entry : completed.entrySet()) {
            if (!definitions.containsKey(entry.getKey())) throw new IllegalArgumentException("Progres merujuk area yang tidak ada: " + entry.getKey());
            entry.getValue().forEach((uuid, count) -> {
                Objects.requireNonNull(uuid);
                if (count == null || count < 1 || count > 100) throw new IllegalArgumentException("Jumlah progres harus 1–100.");
            });
            if (!entry.getValue().isEmpty()) copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        completed = Map.copyOf(copy);
    }

    public static WhisperState empty() { return new WhisperState(Map.of(), Map.of()); }
    public WhisperDefinition require(String id) {
        var definition = definitions.get(id);
        if (definition == null) throw new IllegalArgumentException("Bisikan '" + id + "' tidak ada. Gunakan list atau create.");
        return definition;
    }
    public int count(String id, UUID player) { return completed.getOrDefault(id, Map.of()).getOrDefault(player, 0); }
    public boolean eligible(WhisperDefinition definition, UUID player) {
        return definition.enabled() && definition.targets().contains(player) && count(definition.id(), player) < definition.limit();
    }
    public WhisperState put(WhisperDefinition definition) {
        var next = new HashMap<>(definitions); next.put(definition.id(), definition);
        return new WhisperState(next, completed);
    }
    public WhisperState create(String id) {
        if (definitions.containsKey(id)) throw new IllegalArgumentException("ID sudah dipakai; pilih ID lain atau edit bisikan tersebut.");
        return put(WhisperDefinition.draft(id));
    }
    public WhisperState delete(String id) {
        require(id);
        var next = new HashMap<>(definitions); next.remove(id);
        var progress = new HashMap<>(completed); progress.remove(id);
        return new WhisperState(next, progress);
    }
    public WhisperState finish(WhisperDefinition expected, UUID player) {
        if (!expected.equals(require(expected.id())) || !eligible(expected, player))
            throw new IllegalArgumentException("Bisikan berubah atau penerima tidak lagi memenuhi syarat.");
        var counts = new HashMap<>(completed.getOrDefault(expected.id(), Map.of()));
        counts.put(player, count(expected.id(), player) + 1);
        var progress = new HashMap<>(completed); progress.put(expected.id(), counts);
        return new WhisperState(definitions, progress);
    }
    public WhisperState reset(String id, UUID player) {
        require(id);
        var progress = new HashMap<>(completed);
        if (player == null) progress.remove(id);
        else {
            var counts = new HashMap<>(completed.getOrDefault(id, Map.of())); counts.remove(player);
            progress.put(id, counts);
        }
        return new WhisperState(definitions, progress);
    }
}