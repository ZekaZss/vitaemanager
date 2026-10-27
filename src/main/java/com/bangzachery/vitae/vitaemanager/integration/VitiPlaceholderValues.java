package com.bangzachery.vitae.vitaemanager.integration;

import com.bangzachery.vitae.vitaemanager.economy.VitiAmount;
import com.bangzachery.vitae.vitaemanager.economy.VitiLedger;
import java.math.BigDecimal;
import java.util.UUID;

/** Pure snapshot projection, safe for asynchronous placeholder refreshes. */
public final class VitiPlaceholderValues {
    private VitiPlaceholderValues() {}
    public static String resolve(VitiLedger ledger, UUID player, String params) {
        if (!java.util.Set.of("viti", "viti_raw", "viti_short", "viti_highest").contains(params)) return null;
        var account = player == null ? null : ledger.accounts().get(player);
        BigDecimal amount = account == null ? BigDecimal.ZERO
                : params.equals("viti_highest") ? account.highest() : account.balance();
        return switch (params) {
            case "viti_raw" -> VitiAmount.checked(amount).toPlainString();
            case "viti_short" -> VitiAmount.shortFormat(amount);
            default -> VitiAmount.format(amount);
        };
    }
}
