package com.bangzachery.vitae.vitaemanager.story;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** One atomic file per story module. Never replaces malformed existing data. */
public final class StoryStore<T> {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path path;
    private final Class<T> type;
    public StoryStore(Path path, Class<T> type) { this.path = path; this.type = type; }
    public T load(T initial) throws IOException {
        if (!Files.exists(path)) { save(initial); return initial; }
        try {
            T value = JSON.fromJson(Files.readString(path), type);
            if (value == null) throw new IllegalArgumentException("Isi file kosong.");
            return value;
        } catch (RuntimeException error) { throw new IOException("Data " + path.getFileName() + " tidak valid; file dipertahankan.", error); }
    }
    public void save(T value) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".story-", ".tmp");
        try {
            byte[] bytes = JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException error) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public T copy(T value) { return JSON.fromJson(JSON.toJson(value), type); }
}
