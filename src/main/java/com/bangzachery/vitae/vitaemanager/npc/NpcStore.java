package com.bangzachery.vitae.vitaemanager.npc;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import java.io.*;
import java.lang.reflect.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Invalid files are rejected, never replaced with empty state. */
public final class NpcStore {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path file;
    public NpcStore(Path file) { this.file = file; }
    public NpcData.State initialize() throws IOException {
        if (!Files.exists(file)) { var state = NpcData.State.empty(); save(state); return state; }
        JsonElement json;
        NpcData.State state;
        try (var reader = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.STRICT);
            json = JsonParser.parseReader(reader);
            if (reader.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalArgumentException("Data tambahan setelah JSON.");
            validate(json, NpcData.State.class);
            state = Objects.requireNonNull(JSON.fromJson(json, NpcData.State.class));
        } catch (RuntimeException e) { throw new IOException("npc.json tidak valid. File lama dipertahankan.", e); }
        if (needsUpgrade(json)) {
            Path backup = file.resolveSibling("npc-before-story-update-" + UUID.randomUUID() + ".json.bak");
            Files.copy(file, backup); // Close the reader before replacing a file on Windows.
            save(state);
        }
        return state;
    }
    public void save(NpcData.State state) throws IOException {
        Path parent = file.toAbsolutePath().getParent(); Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "npc-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) { JSON.toJson(state, writer); }
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
    private static boolean needsUpgrade(JsonElement value) {
        if (value.isJsonArray()) { for (JsonElement child : value.getAsJsonArray()) if (needsUpgrade(child)) return true; }
        if (value.isJsonObject()) {
            JsonObject object = value.getAsJsonObject();
            if (object.has("npcs") && !object.has("memories") || object.has("token") && (!object.has("appearance") || !object.has("memory"))
                    || object.has("blocks") && !object.has("timing") || object.has("label") && !object.has("memoryKey")) return true;
            for (var entry : object.entrySet()) if (needsUpgrade(entry.getValue())) return true;
        }
        return false;
    }
    private static void validate(JsonElement value, Type type) {
        if (value == null || value.isJsonNull()) {
            if (type == NpcData.Point.class || type == NpcData.Gate.class || type == com.bangzachery.vitae.vitaemanager.whisper.WhisperArea.class) return;
            throw new IllegalArgumentException("Nilai wajib tidak boleh kosong: " + type);
        }
        if (type instanceof ParameterizedType p) {
            Type[] args = p.getActualTypeArguments();
            if (p.getRawType() == Map.class) {
                if (!value.isJsonObject()) throw new IllegalArgumentException("Map harus berupa object.");
                for (var entry : value.getAsJsonObject().entrySet()) {
                    if (args[0] == UUID.class) UUID.fromString(entry.getKey());
                    validate(entry.getValue(), args[1]);
                }
            } else {
                if (!value.isJsonArray()) throw new IllegalArgumentException("List harus berupa array.");
                for (JsonElement child : value.getAsJsonArray()) validate(child, args[0]);
            }
            return;
        }
        Class<?> c = (Class<?>) type;
        if (c.isRecord()) {
            if (!value.isJsonObject()) throw new IllegalArgumentException("Record harus berupa object.");
            for (RecordComponent field : c.getRecordComponents()) {
                // Older npc.json versions have no orientation offset; their default is zero.
                if (c == NpcData.Definition.class && Set.of("facing", "gate", "appearance", "memory").contains(field.getName()) && !value.getAsJsonObject().has(field.getName())) continue;
                if (c == NpcData.State.class && field.getName().equals("memories") && !value.getAsJsonObject().has(field.getName())) continue;
                if (c == NpcData.Gate.class && field.getName().equals("timing") && !value.getAsJsonObject().has(field.getName())) continue;
                if (c == NpcData.Choice.class && Set.of("memoryKey", "memoryValue").contains(field.getName()) && !value.getAsJsonObject().has(field.getName())) continue;
                validate(value.getAsJsonObject().get(field.getName()), field.getGenericType());
            }
        } else if (c == boolean.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Harus boolean.");
        } else if (c == int.class || c == long.class || c == float.class || c == double.class || c == java.math.BigDecimal.class) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Harus angka.");
            var number = value.getAsBigDecimal();
            if (c == int.class) number.intValueExact(); if (c == long.class) number.longValueExact();
        } else {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Harus teks.");
            if (c == UUID.class) UUID.fromString(value.getAsString());
        }
    }
}
