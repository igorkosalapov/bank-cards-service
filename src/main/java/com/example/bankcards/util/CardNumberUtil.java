package com.example.bankcards.util;

public final class CardNumberUtil {

    private CardNumberUtil() {
    }

    public static String last4(String cardNumber) {
        if (cardNumber == null) {
            throw new IllegalArgumentException("cardNumber is null");
        }
        String digits = cardNumber.replaceAll("\\D", "");
        if (digits.length() < 4) {
            throw new IllegalArgumentException("cardNumber must contain at least 4 digits");
        }
        return digits.substring(digits.length() - 4);
    }

    public static String maskByLast4(String last4) {
        if (last4 == null || last4.length() != 4) {
            throw new IllegalArgumentException("last4 must be 4 digits");
        }
        return "**** **** **** " + last4;
    }
}
