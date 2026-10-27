package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import java.math.BigDecimal;
import java.util.*;

/** Exact prices and immutable, revisioned catalogs. NPC generation UUID prevents ID reuse leaks. */
public final class NpcShopData {
    private NpcShopData() {}
    public enum Mode { SELL, BUY, BOTH }
    public record Price(int base, BigDecimal amount) {
        public Price {
            if (base < 1 || base > 2304) throw new IllegalArgumentException("Jumlah dasar harus 1–2304.");
            amount = VitiAmount.positive(amount);
            try { VitiAmount.positive(amount.divide(BigDecimal.valueOf(base))); }
            catch (ArithmeticException e) { throw new IllegalArgumentException("Harga per satuan harus desimal tepat; gunakan jumlah dasar lain.", e); }
        }
        public BigDecimal total(int quantity) {
            if (quantity < 1 || quantity > 2304) throw new IllegalArgumentException("Jumlah harus 1–2304.");
            return VitiAmount.positive(amount.multiply(BigDecimal.valueOf(quantity)).divide(BigDecimal.valueOf(base)));
        }
    }
    public record Product(String sample, boolean materialOnly, Mode mode, Price sell, Price buy) {
        public Product {
            if (sample == null || sample.isBlank() || sample.length() > 2_000_000) throw new IllegalArgumentException("Contoh item tidak valid.");
            Objects.requireNonNull(mode);
        }
        public boolean available(boolean selling) { return (selling ? mode != Mode.BUY && sell != null : mode != Mode.SELL && buy != null); }
        public Price price(boolean selling) { if (!available(selling)) throw new IllegalArgumentException("Mode/harga produk belum tersedia."); return selling ? sell : buy; }
        public Product withPrice(boolean selling, Price price) { return new Product(sample, materialOnly, mode, selling ? price : sell, selling ? buy : price); }
    }
    public record Catalog(long revision, Map<Integer, Product> products) {
        public Catalog {
            if (revision < 0) throw new IllegalArgumentException("Revisi negatif.");
            products = Map.copyOf(products);
            for (var entry : products.entrySet()) if (entry.getKey() < 0 || entry.getKey() >= 45 || entry.getValue() == null) throw new IllegalArgumentException("Slot toko harus 1–45.");
        }
        public static Catalog empty() { return new Catalog(0, Map.of()); }
        public Catalog edit(long expected, Map<Integer, Product> next) {
            if (revision != expected) throw new IllegalStateException("Katalog diubah admin lain. Buka ulang editor; perubahanmu belum disimpan.");
            return new Catalog(Math.addExact(revision, 1), next);
        }
    }
    public enum Phase { PREPARED, INVENTORY_SAVED, COMPLETE, ABORTED, QUARANTINED }
    public record Receipt(UUID id, UUID player, UUID npc, String npcId, int slot, int quantity,
                          boolean selling, BigDecimal total, long created, List<String> before,
                          List<String> after, String marker, String beforeMarker, Phase phase, String reason) {
        public Receipt {
            Objects.requireNonNull(id); Objects.requireNonNull(player); Objects.requireNonNull(npc); Objects.requireNonNull(npcId);
            total = VitiAmount.positive(total); before = List.copyOf(before); after = List.copyOf(after);
            if (before.size() != 36 || after.size() != 36 || quantity < 1 || quantity > 2304 || slot < 0 || slot >= 45 || created < 0) throw new IllegalArgumentException("Receipt toko tidak valid.");
            if (marker == null || !marker.startsWith(id.toString() + ":")) throw new IllegalArgumentException("Marker receipt tidak valid.");
            if (beforeMarker == null || !beforeMarker.startsWith("preflight:")) throw new IllegalArgumentException("Marker sebelum transaksi tidak valid.");
            Objects.requireNonNull(phase); Objects.requireNonNull(reason);
        }
        public Receipt phase(Phase next, String explanation) { return new Receipt(id, player, npc, npcId, slot, quantity, selling, total, created, before, after, marker, beforeMarker, next, explanation); }
        public boolean terminal() { return phase == Phase.COMPLETE || phase == Phase.ABORTED; }
    }
}
