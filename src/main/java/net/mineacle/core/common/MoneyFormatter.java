package net.mineacle.core.common.format;

public final class MoneyFormatter {

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private MoneyFormatter() {
    }

    public static String compact(double value) {
        boolean negative = value < 0;
        double number = Math.abs(value);

        int suffixIndex = 0;
        while (number >= 1000.0 && suffixIndex < SUFFIXES.length - 1) {
            number /= 1000.0;
            suffixIndex++;
        }

        String formatted;
        if (number >= 100 || number % 1 == 0) {
            formatted = String.format("%.0f", number);
        } else if (number >= 10) {
            formatted = String.format("%.1f", number);
        } else {
            formatted = String.format("%.2f", number);
        }

        formatted = formatted.replaceAll("\\.0$", "").replaceAll("\\.00$", "");

        return (negative ? "-" : "") + formatted + SUFFIXES[suffixIndex];
    }

    public static String money(double value) {
        return "$" + compact(value);
    }
}