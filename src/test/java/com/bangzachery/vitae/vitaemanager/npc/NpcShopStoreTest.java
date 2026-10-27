package com.bangzachery.vitae.vitaemanager.npc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcShopData.*;

class NpcShopStoreTest {
    @TempDir Path root;
    private Receipt receipt() {
        UUID id = UUID.randomUUID(); List<String> inventory = Collections.nCopies(36, "");
        return new Receipt(id, UUID.randomUUID(), UUID.randomUUID(), "shop", 0, 2, true, new BigDecimal("2.65625"), 1, inventory, inventory, id + ":digest", "preflight:" + UUID.randomUUID(), Phase.PREPARED, "");
    }
    @Test void interruptedReceiptResumesAndCompletedAuditIsRetained() throws Exception {
        NpcShopStore first = new NpcShopStore(root); Receipt receipt = receipt(); first.receipt(receipt);
        NpcShopStore restarted = new NpcShopStore(root); assertEquals(receipt, restarted.pending().getFirst());
        Receipt saved = receipt.phase(Phase.INVENTORY_SAVED, "verified"); restarted.receipt(saved);
        NpcShopStore again = new NpcShopStore(root); assertEquals(Phase.INVENTORY_SAVED, again.pending().getFirst().phase()); again.complete(saved); again.complete(saved);
        assertEquals(1, again.pending().size()); assertEquals(Phase.COMPLETE, again.pending().getFirst().phase()); assertTrue(Files.exists(root.resolve("audit").resolve(receipt.id() + ".json")));
    }
    @Test void catalogsAreIndependentAcrossNpcGenerationsAndCopies() throws Exception {
        NpcShopStore store = new NpcShopStore(root); UUID old = UUID.randomUUID(), copy = UUID.randomUUID(), reused = UUID.randomUUID();
        Product product = new Product("item", false, Mode.SELL, new Price(64, new BigDecimal("85")), null);
        store.save(old, 0, Map.of(0, product)); store.save(copy, 0, store.catalog(old).products()); store.save(copy, 1, Map.of());
        NpcShopStore restarted = new NpcShopStore(root); assertEquals(product, restarted.catalog(old).products().get(0)); assertTrue(restarted.catalog(copy).products().isEmpty()); assertTrue(restarted.catalog(reused).products().isEmpty());
        restarted.remove(old); assertTrue(new NpcShopStore(root).catalog(old).products().isEmpty());
    }
    @Test void staleSaveCannotOverwriteOtherAdmin() throws Exception {
        NpcShopStore store = new NpcShopStore(root); UUID npc = UUID.randomUUID(); store.save(npc, 0, Map.of());
        assertThrows(IllegalStateException.class, () -> store.save(npc, 0, Map.of())); assertEquals(1, new NpcShopStore(root).catalog(npc).revision());
    }
    @Test void invalidJournalStopsStoreWithoutDeletingEvidence() throws Exception {
        new NpcShopStore(root); Path file = root.resolve("journal/broken.json"); Files.writeString(file, "{broken");
        assertThrows(IOException.class, () -> new NpcShopStore(root)); assertEquals("{broken", Files.readString(file));
    }
    @Test void invalidCatalogIsNotReplacedByDefaults() throws Exception {
        new NpcShopStore(root); Path file = root.resolve("catalogs.json"); Files.writeString(file, "{\"version\":999,\"catalogs\":{}}");
        assertThrows(IOException.class, () -> new NpcShopStore(root)); assertTrue(Files.readString(file).contains("999"));
    }
    @Test void provenAbortRemainsTerminalAcrossRestartWithoutDeletingJournal() throws Exception {
        NpcShopStore store = new NpcShopStore(root); Receipt receipt = receipt(); store.receipt(receipt); store.abort(receipt);
        Receipt restarted = new NpcShopStore(root).pending().getFirst(); assertTrue(restarted.terminal()); assertEquals(Phase.ABORTED, restarted.phase());
        assertTrue(Files.exists(root.resolve("journal").resolve(receipt.id() + ".json"))); assertTrue(Files.exists(root.resolve("audit").resolve(receipt.id() + ".json")));
    }
}
