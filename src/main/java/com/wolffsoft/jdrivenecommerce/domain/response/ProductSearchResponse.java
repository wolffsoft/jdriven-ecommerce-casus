package com.wolffsoft.jdrivenecommerce.domain.response;

import java.math.BigDecimal;
import java.util.Map;

public record ProductSearchResponse(
        String id,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Map<String, String> attributes
) {}
