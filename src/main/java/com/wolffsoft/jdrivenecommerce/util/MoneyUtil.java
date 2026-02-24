package com.wolffsoft.jdrivenecommerce.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MoneyUtil {

    public static long toCents(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is null");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact();
    }

    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2);
    }
}
