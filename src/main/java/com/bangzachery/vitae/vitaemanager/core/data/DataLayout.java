package com.bangzachery.vitae.vitaemanager.core.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Copy-first layout migration. The retained source is archival after its fingerprint is committed. */
public final class DataLayout {
    private DataLayout() {}

    public static void initialize(Path root) throws IOException {
        migrate(root, "server-control.yml", "server/state.yml");
        migrate(root, "totem-settings.yml", "totem/settings.yml");
        migrate(root, "whispers.yml", "whisper/whispers.yml");
        migrate(root, "evolution.yml", "evolution/evolution.yml");
        migrate(root, "npc.json", "npc/npc.json");
        migrate(root, "dungeons.json", "story/dungeons.json");
        migrate(root, "fade.json", "story/fade.json");
        migrate(root, "music.yml", "music/music.yml");
        // viti.yml, its schema/conversion markers, original backups and spent receipt log stay together.
    }

    public static void migrate(Path root, String legacy, String destination) throws IOException {
        Path base = root.toAbsolutePath().normalize();
        Path source = inside(base, legacy), target = inside(base, destination);
        Path backup = inside(base, "migration/layout-backup/" + legacy);
        Path receipt = inside(base, "migration/layout-journal/" + legacy + ".sha256");
        Path completed = inside(base, "migration/layout-journal/" + legacy + ".complete");
        if (!Files.exists(source)) {
            if (Files.exists(receipt) && !Files.exists(target))
                throw new IOException("Migrasi kehilangan sumber dan tujuan: " + source + " -> " + target);
            return;
        }
        if (!Files.isRegularFile(source) || Files.isSymbolicLink(source))
            throw new IOException("Sumber migrasi bukan file biasa: " + source);
        byte[] original = Files.readAllBytes(source);
        String fingerprint = hash(original);
        if (Files.exists(receipt)) {
            if (!Files.readString(receipt).equals(fingerprint))
                throw new IOException("File lama berubah setelah migrasi; selesaikan konflik manual: " + source + " dan " + target);
            if (!Files.exists(backup) || !hash(Files.readAllBytes(backup)).equals(fingerprint))
                throw new IOException("Backup migrasi tidak valid: " + backup);
            if (Files.exists(completed)) {
                if (!Files.readString(completed).equals(fingerprint))
                    throw new IOException("Marker migrasi tidak valid: " + completed);
                if (Files.exists(target)) return; // target is authoritative only after verified completion
                throw new IOException("File aktif hasil migrasi hilang; jangan pulihkan salinan lama otomatis: " + target);
            } else if (Files.exists(target) && !hash(Files.readAllBytes(target)).equals(fingerprint)) {
                throw new IOException("Salinan migrasi belum terverifikasi dan bertentangan: " + source + " dan " + target);
            }
        } else {
            if (Files.exists(target) && !hash(Files.readAllBytes(target)).equals(fingerprint))
                throw new IOException("Konflik migrasi; kedua file dipertahankan: " + source + " dan " + target);
            if (Files.exists(backup)) {
                if (!hash(Files.readAllBytes(backup)).equals(fingerprint))
                    throw new IOException("Backup migrasi bertentangan: " + backup);
            } else atomicCreate(backup, original);
            // PREPARED receipt is durable before publishing the copy; retry can resume a missing target.
            atomicCreate(receipt, fingerprint.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        if (!Files.exists(target)) atomicCreate(target, original);
        if (!hash(Files.readAllBytes(target)).equals(fingerprint))
            throw new IOException("Verifikasi salinan gagal: " + target);
        if (!Files.exists(completed)) atomicCreate(completed, fingerprint.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static Path inside(Path root, String relative) throws IOException {
        Path path = root.resolve(relative).normalize();
        if (!path.startsWith(root) || path.equals(root)) throw new IOException("Lokasi migrasi tidak valid");
        for (Path part = path; part != null && part.startsWith(root); part = part.getParent())
            if (Files.isSymbolicLink(part)) throw new IOException("Symlink migrasi ditolak: " + part);
        return path;
    }

    private static void atomicCreate(Path file, byte[] bytes) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), ".layout-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            // No replacement: an unexpected concurrent destination must be reviewed.
            if (Files.exists(file)) throw new IOException("Tujuan migrasi muncul saat penyalinan: " + file);
            Files.move(temp, file);
        } finally { Files.deleteIfExists(temp); }
    }

    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
