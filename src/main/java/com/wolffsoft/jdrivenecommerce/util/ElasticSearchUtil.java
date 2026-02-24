package com.wolffsoft.jdrivenecommerce.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ElasticSearchUtil {

    public static String buildPriceText(Long priceInCents, String currency) {
        if (priceInCents == null) {
            return currency == null ? "" : currency;
        }

        // Provide a few text tokens so single-field search can match "1999", "19.99", and currency.
        BigDecimal major = BigDecimal.valueOf(priceInCents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        String centsToken = String.valueOf(priceInCents);
        String majorToken = major.toPlainString();
        String currencyToken = currency == null ? "" : currency;

        return (centsToken + " " + majorToken + " " + currencyToken).trim();
    }
}
