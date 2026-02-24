package com.wolffsoft.jdrivenecommerce.elasticsearch;

import java.util.Map;

public record ProductSearchDocument(
        String id,
        String name,
        String description,
        Long priceInCents,
        String currency,
        String currencyText,
        String priceText,
        Map<String, String> attributes,
        String attributesText) {
}
