package com.bangzachery.vitae.vitaemanager.economy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VitiLedgerTest {
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();

    @Test
    void acceptsIndonesianAmountsAndFormatsWithoutRounding() {
        assertEquals(
                new BigDecimal("1000"),
                VitiAmount.parse("1.000").setScale(0));

        assertEquals(
                new BigDecimal("1000.5"),
                VitiAmount.parse("1.000,50"));

        assertEquals(
                "1.000,5",
                VitiAmount.format(VitiAmount.parse("1000,50")));

        assertEquals(
                "0,123456789",
                VitiAmount.format(
                        VitiAmount.stored("0.123456789")));
    }

    @Test
    void rejectsAmbiguousInvalidNegativeAndNonFiniteInputs() {
        for (String input : new String[]{
                "NaN", "Infinity", "-1", "+1", "1e3",
                "1.50", "1,000", "1.00.000", " 10", "10 ", ""}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> VitiAmount.parse(input),
                    input);
        }

        for (Object value : new Object[]{
                Double.NaN, Double.POSITIVE_INFINITY,
                -1.0, true, "1e309"}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> VitiAmount.stored(value));
        }
    }

    @Test
    void preservesSmallAndLargeFiniteLegacyDoubleAmounts() {
        assertEquals(
                BigDecimal.valueOf(Double.MIN_VALUE),
                VitiAmount.stored(Double.MIN_VALUE));

        assertEquals(
                BigDecimal.valueOf(Double.MAX_VALUE),
                VitiAmount.stored(Double.MAX_VALUE));

        assertFalse(
                VitiAmount.format(
                                VitiAmount.stored(Double.MIN_VALUE))
                        .equals("0"));
    }

    @Test
    void decimalArithmeticIsExact() {
        VitiLedger ledger = ready("0,10", "0,20");

        VitiLedger result = ledger.transfer(
                first, second, VitiAmount.parse("0,10"));

        assertEquals(BigDecimal.ZERO, result.balance(first));
        assertEquals(new BigDecimal("0.3"), result.balance(second));
        assertEquals(new BigDecimal("0.1"), ledger.balance(first));
    }

    @Test
    void transferConservesTotalAndPreservesHistoricPeak() {
        VitiLedger ledger = ready("100", "20");

        VitiLedger changed = ledger.transfer(
                first, second, VitiAmount.parse("30"));

        assertEquals(
                new BigDecimal("120"),
                changed.balance(first)
                        .add(changed.balance(second)).setScale(0));

        assertEquals(
                VitiAmount.parse("100"),
                changed.accounts().get(first).highest());

        assertEquals(
                VitiAmount.parse("50"),
                changed.accounts().get(second).highest());

        assertEquals(first, changed.top().getFirst().getKey());
    }

    @Test
    void insufficientTransferAndSelfTransferLeaveOriginalUntouched() {
        VitiLedger ledger = ready("10", "20");

        assertEquals(
                "viti-insufficient",
                assertThrows(
                        VitiLedger.Failure.class,
                        () -> ledger.transfer(
                                first, second,
                                VitiAmount.parse("11"))).key());

        assertThrows(
                VitiLedger.Failure.class,
                () -> ledger.transfer(
                        first, first, BigDecimal.ONE));

        assertEquals(VitiAmount.parse("10"), ledger.balance(first));
        assertEquals(VitiAmount.parse("20"), ledger.balance(second));
    }

    @Test
    void targetOverflowCannotCommitSenderDebit() {
        VitiLedger ledger = ready("10", "0")
                .set(second, VitiAmount.stored(Double.MAX_VALUE));

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.transfer(
                        first, second, BigDecimal.ONE));

        assertEquals(VitiAmount.parse("10"), ledger.balance(first));
    }

    @Test
    void adminSetZeroRetainsPeakAndRemoveDoesNotClampSilently() {
        VitiLedger ledger = ready("100", "0");
        VitiLedger zero = ledger.set(first, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, zero.balance(first));

        assertEquals(
                VitiAmount.parse("100"),
                zero.accounts().get(first).highest());

        assertThrows(
                VitiLedger.Failure.class,
                () -> zero.remove(first, BigDecimal.ONE));

        assertThrows(
                IllegalArgumentException.class,
                () -> ledger.add(first, BigDecimal.ZERO));
    }

    @Test
    void withdrawalReservesValueUntilDeliveredAndRedeemed() {
        UUID serial = UUID.randomUUID();

        VitiLedger withdrawn = ready("100", "0")
                .withdraw(first, serial, VitiAmount.parse("40"));

        assertEquals(
                VitiAmount.parse("60"), withdrawn.balance(first));

        assertTrue(withdrawn.notes().get(serial).pending());

        VitiLedger delivered = withdrawn.delivered(Set.of(serial));

        assertFalse(delivered.notes().get(serial).pending());

        VitiLedger paid = delivered.redeem(
                second, serial, VitiAmount.parse("40"), false);

        assertEquals(VitiAmount.parse("40"), paid.balance(second));
        assertFalse(paid.notes().containsKey(serial));
        assertTrue(paid.redeemed().contains(serial));
    }

    @Test
    void duplicatePaperCannotPayAgainEvenWhenTaggedLegacy() {
        UUID serial = UUID.randomUUID();

        VitiLedger paid = ready("100", "0")
                .withdraw(first, serial, BigDecimal.TEN)
                .redeem(second, serial, BigDecimal.TEN, false);

        assertThrows(
                VitiLedger.Failure.class,
                () -> paid.redeem(
                        first, serial, BigDecimal.TEN, false));

        assertThrows(
                VitiLedger.Failure.class,
                () -> paid.redeem(
                        first, serial, BigDecimal.TEN, true));
    }

    @Test
    void legacyPaperPaysOnceAndUnknownNewPaperNeverPays() {
        UUID legacyId = UUID.randomUUID();
        VitiLedger ledger = ready("0", "0");

        assertThrows(
                VitiLedger.Failure.class,
                () -> ledger.redeem(
                        first, legacyId, BigDecimal.TEN, false));

        VitiLedger paid = ledger.redeem(
                first, legacyId, BigDecimal.TEN, true);

        assertEquals(
                BigDecimal.TEN.stripTrailingZeros(),
                paid.balance(first));

        assertThrows(
                VitiLedger.Failure.class,
                () -> paid.redeem(
                        second, legacyId, BigDecimal.TEN, true));
    }

    @Test
    void forgedAmountAndReusedSerialCannotChangeLedger() {
        UUID serial = UUID.randomUUID();

        VitiLedger ledger = ready("100", "0")
                .withdraw(first, serial, BigDecimal.TEN);

        assertThrows(
                VitiLedger.Failure.class,
                () -> ledger.redeem(
                        second, serial, BigDecimal.ONE, false));

        assertThrows(
                VitiLedger.Failure.class,
                () -> ledger.withdraw(
                        first, serial, BigDecimal.ONE));

        assertTrue(ledger.notes().containsKey(serial));
    }

    @Test
    void reloadablePendingStateCanBeAcknowledgedWithoutAnotherDebit() {
        UUID serial = UUID.randomUUID();

        VitiLedger ledger = ready("100", "0")
                .withdraw(first, serial, BigDecimal.TEN);

        VitiLedger ack = ledger.delivered(Set.of(serial));

        assertEquals(ledger.balance(first), ack.balance(first));
        assertEquals(ack, ack.delivered(Set.of(serial)));
    }

    @Test
    void snapshotsCopyMapsAndReceiptsAndRankingBreaksTiesByUuid() {
        var input = new HashMap<UUID, VitiLedger.Account>();

        input.put(
                first,
                new VitiLedger.Account(
                        "One", BigDecimal.ZERO, BigDecimal.TEN));

        input.put(
                second,
                new VitiLedger.Account(
                        "Two", BigDecimal.ONE, BigDecimal.TEN));

        VitiLedger ledger =
                new VitiLedger(input, Map.of(), Set.of());

        input.clear();

        assertEquals(2, ledger.accounts().size());

        assertThrows(
                UnsupportedOperationException.class,
                () -> ledger.accounts().clear());

        assertThrows(
                UnsupportedOperationException.class,
                () -> ledger.redeemed().add(UUID.randomUUID()));

        UUID expected =
                first.compareTo(second) < 0 ? first : second;

        assertEquals(
                expected, ledger.top().getFirst().getKey());
    }

    private VitiLedger ready(String a, String b) {
        return new VitiLedger(Map.of(), Map.of(), Set.of())
                .seed(first, "One", VitiAmount.parse(a))
                .seed(second, "Two", VitiAmount.parse(b));
    }
}