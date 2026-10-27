package com.bangzachery.vitae.vitaemanager.economy;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class VitiFormatTest {
    @Test void shortDisplayNeverChangesTheExactAmount() {
        String[][] cases = {{"950", "950"}, {"1000", "1k"}, {"1500", "1,5k"},
                {"100000", "100k"}, {"1000000", "1jt"}, {"12500.75", "12,5k"},
                {"2.65625", "2,65625"}, {"1000000000", "1.000jt"}};
        for (var c : cases) {
            BigDecimal amount = new BigDecimal(c[0]);
            assertEquals(c[1], VitiAmount.shortFormat(amount));
            assertEquals(c[0], amount.toPlainString());
        }
        assertEquals("12.500,75", VitiAmount.format(new BigDecimal("12500.75")));
    }
}
