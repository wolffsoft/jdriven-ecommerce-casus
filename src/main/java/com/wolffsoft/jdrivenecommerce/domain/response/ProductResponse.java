package com.wolffsoft.jdrivenecommerce.domain.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ProductResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Map<String, String> attributes,
        Instant priceUpdatedAt
) {}
