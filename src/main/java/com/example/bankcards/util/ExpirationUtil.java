package com.example.bankcards.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class ExpirationUtil {

    private static final DateTimeFormatter YYYY_MM = DateTimeFormatter.ofPattern("yyyy-MM");

    private ExpirationUtil() {
    }

    public static LocalDate parseToLastDayOfMonth(String expiration) {
        if (expiration == null) {
            throw new IllegalArgumentException("expiration is null");
        }
        String v = expiration.trim();

        try {
            YearMonth ym = YearMonth.parse(v, YYYY_MM);
            return ym.atEndOfMonth();
        } catch (DateTimeParseException ignored) {
        }

        if (v.matches("\\d{2}/\\d{2}")) {
            int month = Integer.parseInt(v.substring(0, 2));
            int year = 2000 + Integer.parseInt(v.substring(3, 5));
            return YearMonth.of(year, month).atEndOfMonth();
        }
        if (v.matches("\\d{2}/\\d{4}")) {
            int month = Integer.parseInt(v.substring(0, 2));
            int year = Integer.parseInt(v.substring(3, 7));
            return YearMonth.of(year, month).atEndOfMonth();
        }

        throw new IllegalArgumentException("Unsupported expiration format: " + expiration);
    }

    public static String formatToMmYy(LocalDate expirationDate) {
        if (expirationDate == null) {
            return null;
        }
        int month = expirationDate.getMonthValue();
        int year2 = expirationDate.getYear() % 100;
        return String.format("%02d/%02d", month, year2);
    }
}
