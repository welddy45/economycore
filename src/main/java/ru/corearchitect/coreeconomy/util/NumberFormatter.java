package ru.corearchitect.coreeconomy.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class NumberFormatter {

    private static final DecimalFormat FORMAT;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.GERMAN);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        FORMAT = new DecimalFormat("#,##0.00", symbols);
    }

    private NumberFormatter() {
    }

    public static String format(BigDecimal amount) {
        return FORMAT.format(amount);
    }
}