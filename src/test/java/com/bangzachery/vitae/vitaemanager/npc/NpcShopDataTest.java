package com.bangzachery.vitae.vitaemanager.npc;

import com.bangzachery.vitae.vitaemanager.economy.VitiLedger;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.bangzachery.vitae.vitaemanager.npc.NpcShopData.*;

class NpcShopDataTest {
    @Test void carrotFractionRemainsExact() { assertEquals(new BigDecimal("2.65625"), new Price(64, new BigDecimal("85")).total(2)); }
    @Test void allTemplateFractionsAreFiniteAndExact() { for (Price price : List.of(new Price(64, new BigDecimal("85")), new Price(32, new BigDecimal("75")), new Price(20, new BigDecimal("40")), new Price(16, new BigDecimal("85")))) assertEquals(0, price.amount().compareTo(price.total(price.base()))); }
    @Test void refusesRepeatingDecimalRatherThanRounding() { assertThrows(IllegalArgumentException.class, () -> new Price(3, BigDecimal.ONE)); }
    @Test void rejectsStaleEditorAndKeepsOriginal() {
        Catalog first = Catalog.empty(); Product product = new Product("item", false, Mode.BOTH, null, null);
        Catalog saved = first.edit(0, Map.of(0, product)); assertThrows(IllegalStateException.class, () -> saved.edit(0, Map.of())); assertEquals(product, saved.products().get(0));
    }
    @Test void debitAndCreditReceiptsCannotRepeatAfterReload() {
        UUID player = UUID.randomUUID(), sell = UUID.randomUUID(), buy = UUID.randomUUID();
        VitiLedger initial = new VitiLedger(Map.of(), Map.of(), Set.of()).seed(player, "Shopper", new BigDecimal("10"));
        VitiLedger credited = initial.shopExchange(player, sell, new BigDecimal("2.65625"), true);
        VitiLedger debited = credited.shopExchange(player, buy, new BigDecimal("5.3125"), false);
        VitiLedger reloaded = new VitiLedger(debited.accounts(), debited.notes(), debited.redeemed(), debited.giveaway());
        assertEquals(new BigDecimal("7.34375"), reloaded.balance(player)); assertSame(reloaded, reloaded.shopExchange(player, sell, new BigDecimal("2.65625"), true)); assertSame(reloaded, reloaded.shopExchange(player, buy, new BigDecimal("5.3125"), false));
    }
    @Test void insufficientDebitDoesNotCreateReceipt() {
        UUID player = UUID.randomUUID(), receipt = UUID.randomUUID(); VitiLedger ledger = new VitiLedger(Map.of(), Map.of(), Set.of()).seed(player, "Shopper", BigDecimal.ONE);
        assertThrows(VitiLedger.Failure.class, () -> ledger.shopExchange(player, receipt, BigDecimal.TEN, false)); assertFalse(ledger.redeemed().contains(receipt)); assertEquals(BigDecimal.ONE, ledger.balance(player));
    }
}
