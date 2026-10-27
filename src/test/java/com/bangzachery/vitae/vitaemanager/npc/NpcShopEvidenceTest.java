package com.bangzachery.vitae.vitaemanager.npc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class NpcShopEvidenceTest {
    @TempDir Path root;
    private byte[] item(int amount, String custom, boolean slot) throws IOException {
        var bytes = new ByteArrayOutputStream(); try (var out = new DataOutputStream(bytes)) { out.writeByte(10); out.writeUTF(""); itemFields(out, amount, custom, slot); out.writeByte(0); } return bytes.toByteArray();
    }
    private void itemFields(DataOutputStream out, int amount, String custom, boolean slot) throws IOException {
        if (slot) { out.writeByte(1); out.writeUTF("Slot"); out.writeByte(0); }
        out.writeByte(8); out.writeUTF("id"); out.writeUTF("minecraft:carrot"); out.writeByte(3); out.writeUTF("count"); out.writeInt(amount);
        out.writeByte(10); out.writeUTF("components"); out.writeByte(8); out.writeUTF("minecraft:custom_name"); out.writeUTF(custom); out.writeByte(0);
        if (!slot) { out.writeByte(3); out.writeUTF("DataVersion"); out.writeInt(4189); }
    }
    private Path player(String marker, int amount, String custom) throws IOException {
        Path path = root.resolve("player.dat");
        try (var out = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(path)))) {
            out.writeByte(10); out.writeUTF(""); out.writeByte(10); out.writeUTF("BukkitValues"); out.writeByte(8); out.writeUTF("vitaemanager:npc_shop_receipt"); out.writeUTF(marker); out.writeByte(0);
            out.writeByte(9); out.writeUTF("Inventory"); out.writeByte(10); out.writeInt(1); itemFields(out, amount, custom, true); out.writeByte(0); out.writeByte(0);
        } return path;
    }
    private List<String> inventory(int amount, String custom) throws IOException { var list = new ArrayList<>(Collections.nCopies(36, "")); list.set(0, Base64.getEncoder().encodeToString(item(amount, custom, false))); return list; }
    @Test void verifiesMarkerAndFullNativeInventoryMetadata() throws Exception { assertTrue(NpcShopEvidence.verified(player("receipt", 2, "custom"), "vitaemanager:npc_shop_receipt", "receipt", inventory(2, "custom"))); }
    @Test void markerAloneDoesNotProveAmount() throws Exception { assertFalse(NpcShopEvidence.verified(player("receipt", 1, "custom"), "vitaemanager:npc_shop_receipt", "receipt", inventory(2, "custom"))); }
    @Test void markerAloneDoesNotProveCustomMetadata() throws Exception { assertFalse(NpcShopEvidence.verified(player("receipt", 2, "other"), "vitaemanager:npc_shop_receipt", "receipt", inventory(2, "custom"))); }
    @Test void correctInventoryWithoutReceiptMarkerIsNotEvidence() throws Exception { assertFalse(NpcShopEvidence.verified(player("old", 2, "custom"), "vitaemanager:npc_shop_receipt", "receipt", inventory(2, "custom"))); }
    @Test void rejectsMissingOrMalformedPlayerdata() throws Exception { assertFalse(NpcShopEvidence.verified(root.resolve("missing.dat"), "k", "m", inventory(1, "x"))); assertThrows(IOException.class, () -> NpcShopEvidence.read(new byte[]{10,0})); }
    @Test void rejectsUnboundedNbtLength() throws Exception { var bytes = new ByteArrayOutputStream(); try (var out = new DataOutputStream(bytes)) { out.writeByte(10); out.writeUTF(""); out.writeByte(7); out.writeUTF("huge"); out.writeInt(Integer.MAX_VALUE); } assertThrows(IOException.class, () -> NpcShopEvidence.read(bytes.toByteArray())); }
}
