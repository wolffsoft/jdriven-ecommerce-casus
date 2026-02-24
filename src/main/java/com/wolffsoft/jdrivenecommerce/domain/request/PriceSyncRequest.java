package com.wolffsoft.jdrivenecommerce.domain.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record PriceSyncRequest(
        @NotBlank String requestId,
        @NotNull UUID productId,
        @NotNull @Min(0) Long priceInCents,
        @NotBlank String currency,
        @NotNull Instant effectiveAt,
        String source) {}
