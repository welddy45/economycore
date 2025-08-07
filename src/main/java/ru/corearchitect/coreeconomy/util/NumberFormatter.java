package ru.corearchitect.coreeconomy.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class NumberFormatter {

    private static final ThreadLocal<DecimalFormat> FORMATTER = ThreadLocal.withInitial(() -> {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols);
    });

    private NumberFormatter() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "0,00";
        }
        return FORMATTER.get().format(amount);
    }
}