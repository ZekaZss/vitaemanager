package com.bangzachery.vitae.vitaemanager.economy;

import java.math.BigDecimal;

public final class VitiAmount {
    private static final BigDecimal MAX =
            BigDecimal.valueOf(Double.MAX_VALUE);

    private VitiAmount() {}

    public static BigDecimal parse(String input) {
        if (input == null
                || input.length() > 400
                || !input.matches(
                "(?:[0-9]+|[1-9][0-9]{0,2}(?:\\.[0-9]{3})+)(?:,[0-9]{1,2})?")) {
            throw new IllegalArgumentException(
                    "Gunakan 1000, 1.000, atau 1.000,50");
        }

        return checked(new BigDecimal(
                input.replace(".", "").replace(',', '.')));
    }

    public static BigDecimal stored(Object value) {
        if (!(value instanceof Number)
                && !(value instanceof String)) {
            throw new IllegalArgumentException(
                    "Nominal harus angka atau teks desimal");
        }

        String text = value.toString();

        if (text.length() > 400) {
            throw new IllegalArgumentException(
                    "Nominal terlalu panjang");
        }

        return checked(new BigDecimal(text));
    }

    public static BigDecimal checked(BigDecimal value) {
        if (value == null
                || value.signum() < 0
                || value.compareTo(MAX) > 0
                || value.scale() > 340
                || value.scale() < -308
                || value.precision() > 400) {
            throw new IllegalArgumentException(
                    "Nominal negatif, tidak finite, atau di luar rentang");
        }

        return value.signum() == 0
                ? BigDecimal.ZERO
                : value.stripTrailingZeros();
    }

    public static BigDecimal positive(BigDecimal value) {
        BigDecimal checked = checked(value);

        if (checked.signum() == 0) {
            throw new IllegalArgumentException(
                    "Nominal harus lebih dari nol");
        }

        return checked;
    }

    public static String format(BigDecimal value) {
        String[] parts = checked(value)
                .toPlainString().split("\\.", 2);

        String integer = parts[0].replaceAll(
                "(?<=\\d)(?=(\\d{3})+$)", ".");

        return integer
                + (parts.length == 2 ? "," + parts[1] : "");
    }
}