package com.bangzachery.vitae.vitaemanager.npc;

import com.google.gson.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcShopData.*;

/** Forced atomic writes. Completed receipts and inventory evidence are retained for audit. */
public final class NpcShopStore {
    private final Path root;
    private Map<UUID, Catalog> catalogs;
    public NpcShopStore(Path root) throws IOException {
        this.root = root; Files.createDirectories(root.resolve("journal")); Files.createDirectories(root.resolve("audit"));
        Path file = root.resolve("catalogs.json"); catalogs = new HashMap<>();
        if (Files.exists(file)) {
            try {
                JsonObject object = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                if (object.get("version").getAsInt() != 1) throw new IllegalArgumentException("Versi katalog tidak dikenal.");
                for (var entry : object.getAsJsonObject("catalogs").entrySet()) catalogs.put(UUID.fromString(entry.getKey()), NpcStore.JSON.fromJson(entry.getValue(), Catalog.class));
            } catch (RuntimeException e) { throw new IOException("Katalog toko rusak; file dipertahankan.", e); }
        } else saveCatalogs(catalogs);
        // Parse every journal before accepting new exchanges, including orphaned/offline players.
        pending();
    }
    public Catalog catalog(UUID token) { return catalogs.getOrDefault(token, Catalog.empty()); }
    public void save(UUID token, long revision, Map<Integer, Product> products) throws IOException {
        Catalog next = catalog(token).edit(revision, products); var copy = new HashMap<>(catalogs); copy.put(token, next); saveCatalogs(copy); catalogs = copy;
    }
    public void remove(UUID token) throws IOException { var copy = new HashMap<>(catalogs); copy.remove(token); saveCatalogs(copy); catalogs = copy; }
    private void saveCatalogs(Map<UUID, Catalog> values) throws IOException {
        JsonObject object = new JsonObject(); object.addProperty("version", 1); object.add("catalogs", NpcStore.JSON.toJsonTree(values)); write(root.resolve("catalogs.json"), object);
    }
    public void receipt(Receipt receipt) throws IOException { write(root.resolve("journal").resolve(receipt.id() + ".json"), NpcStore.JSON.toJsonTree(receipt)); }
    public List<Receipt> pending() throws IOException {
        return readJournal(root.resolve("journal"));
    }
    /** Also used by economy startup before NPC definitions/renderers can fail to load. */
    public static List<Receipt> readJournal(Path journal) throws IOException {
        List<Receipt> result = new ArrayList<>();
        if (!Files.exists(journal)) return result;
        try (var stream = Files.list(journal)) {
            for (Path file : stream.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                try {
                    Receipt receipt = Objects.requireNonNull(NpcStore.JSON.fromJson(Files.readString(file), Receipt.class));
                    if (!file.getFileName().toString().equals(receipt.id() + ".json")) throw new IllegalArgumentException("ID receipt/file berbeda.");
                    result.add(receipt);
                } catch (RuntimeException e) { throw new IOException("Journal toko tidak valid: " + file + "; data dipertahankan.", e); }
            }
        }
        return result;
    }
    public void complete(Receipt receipt) throws IOException {
        // Audit first, then terminal journal. Retry safely overwrites the same receipt, never deletes it.
        Receipt completed = receipt.phase(Phase.COMPLETE, "Inventory marker diverifikasi pada playerdata; receipt ledger tersimpan.");
        write(root.resolve("audit").resolve(receipt.id() + ".json"), NpcStore.JSON.toJsonTree(completed)); receipt(completed);
    }
    public void abort(Receipt receipt) throws IOException {
        Receipt aborted = receipt.phase(Phase.ABORTED, "Snapshot sebelum transaksi dan marker native terbukti; ledger belum diproses.");
        write(root.resolve("audit").resolve(receipt.id() + ".json"), NpcStore.JSON.toJsonTree(aborted)); receipt(aborted);
    }
    private static void write(Path file, JsonElement value) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent()); Path temp = Files.createTempFile(file.toAbsolutePath().getParent(), "shop-", ".tmp");
        try {
            Files.writeString(temp, NpcStore.JSON.toJson(value), StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) { channel.force(true); }
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
