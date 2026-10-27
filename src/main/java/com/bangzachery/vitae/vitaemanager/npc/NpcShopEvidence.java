package com.bangzachery.vitae.vitaemanager.npc;

import java.io.*;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Read-only vanilla NBT verification, with bounded allocation; never edits native playerdata. */
public final class NpcShopEvidence {
    private NpcShopEvidence() {}
    public static boolean verified(Path playerdata, String key, String marker, List<String> inventory) throws IOException {
        if (!Files.isRegularFile(playerdata)) return false;
        if (Files.size(playerdata) > 32 * 1024 * 1024) throw new IOException("Playerdata melebihi batas verifikasi toko.");
        // Native saving may report success without surfacing an I/O failure. Flush the actual
        // saved file before recording a durable INVENTORY_SAVED journal acknowledgement.
        try (FileChannel channel = FileChannel.open(playerdata, StandardOpenOption.WRITE)) { channel.force(true); }
        Map<String, Object> root = compound(read(Files.readAllBytes(playerdata)));
        Object values = root.get("BukkitValues");
        if (!(values instanceof Map<?, ?> pdc) || !marker.equals(pdc.get(key))) return false;
        Object items = root.get("Inventory");
        if (!(items instanceof List<?> list)) return false;
        Map<Integer, Map<String, Object>> actual = new HashMap<>();
        for (Object item : list) {
            Map<String, Object> data = compound(item); Object slot = data.get("Slot");
            if (!(slot instanceof Byte number)) return false;
            int index = Byte.toUnsignedInt(number);
            if (index < 36 && actual.put(index, normalized(data)) != null) return false;
        }
        if (inventory.size() != 36) return false;
        for (int i = 0; i < 36; i++) {
            String encoded = inventory.get(i);
            if (encoded.isEmpty()) { if (actual.containsKey(i)) return false; }
            else if (!normalized(compound(read(Base64.getDecoder().decode(encoded)))).equals(actual.get(i))) return false;
        }
        return true;
    }
    private static Map<String, Object> normalized(Map<String, Object> input) {
        var result = new HashMap<>(input); result.remove("Slot"); result.remove("DataVersion"); return result;
    }
    @SuppressWarnings("unchecked") private static Map<String, Object> compound(Object input) throws IOException {
        if (!(input instanceof Map<?, ?>)) throw new IOException("NBT compound diperlukan."); return (Map<String, Object>) input;
    }
    static Object read(byte[] bytes) throws IOException {
        if (bytes.length > 32 * 1024 * 1024) throw new IOException("NBT terlalu besar.");
        InputStream source = new ByteArrayInputStream(bytes);
        if (bytes.length >= 2 && (bytes[0] & 255) == 31 && (bytes[1] & 255) == 139) source = new GZIPInputStream(source);
        try (DataInputStream in = new DataInputStream(new LimitedInput(source))) {
            int type = in.readUnsignedByte(); if (type != 10) throw new IOException("Root NBT bukan compound."); in.readUTF(); return value(in, type, 0);
        }
    }
    private static int length(DataInputStream in) throws IOException { int size = in.readInt(); if (size < 0 || size > 1_000_000) throw new IOException("Panjang NBT tidak valid."); return size; }
    private static Object value(DataInputStream in, int type, int depth) throws IOException {
        if (depth > 128) throw new IOException("NBT terlalu dalam.");
        return switch (type) {
            case 1 -> in.readByte(); case 2 -> in.readShort(); case 3 -> in.readInt(); case 4 -> in.readLong(); case 5 -> in.readFloat(); case 6 -> in.readDouble(); case 8 -> in.readUTF();
            case 7 -> { int size = length(in); List<Byte> data = new ArrayList<>(size); for (int i = 0; i < size; i++) data.add(in.readByte()); yield data; }
            case 9 -> { int child = in.readUnsignedByte(), size = length(in); List<Object> list = new ArrayList<>(size); for (int i = 0; i < size; i++) list.add(value(in, child, depth + 1)); yield list; }
            case 10 -> { Map<String, Object> map = new HashMap<>(); int child; while ((child = in.readUnsignedByte()) != 0) { String name = in.readUTF(); if (map.size() >= 100_000 || map.put(name, value(in, child, depth + 1)) != null) throw new IOException("Compound NBT tidak valid."); } yield map; }
            case 11 -> { int size = length(in); List<Integer> data = new ArrayList<>(size); for (int i = 0; i < size; i++) data.add(in.readInt()); yield data; }
            case 12 -> { int size = length(in); List<Long> data = new ArrayList<>(size); for (int i = 0; i < size; i++) data.add(in.readLong()); yield data; }
            default -> throw new IOException("Jenis NBT tidak dikenal: " + type);
        };
    }
    private static final class LimitedInput extends FilterInputStream {
        int remaining = 32 * 1024 * 1024;
        LimitedInput(InputStream input) { super(input); }
        @Override public int read() throws IOException { if (--remaining < 0) throw new IOException("NBT melebihi batas."); return super.read(); }
        @Override public int read(byte[] buffer, int offset, int count) throws IOException { if (remaining <= 0) throw new IOException("NBT melebihi batas."); int read = super.read(buffer, offset, Math.min(count, remaining)); if (read > 0) remaining -= read; return read; }
    }
}
