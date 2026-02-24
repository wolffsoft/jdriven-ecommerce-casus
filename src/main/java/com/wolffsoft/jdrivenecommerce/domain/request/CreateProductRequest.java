package com.wolffsoft.jdrivenecommerce.domain.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Map;

public record CreateProductRequest(
        @NotBlank String name,
        String description,
        @NotNull BigDecimal price,
        @NotBlank String currency,
        Map<String, String> attributes
) {}
