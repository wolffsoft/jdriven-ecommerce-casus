package com.wolffsoft.jdrivenecommerce.domain.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdatePriceRequest(
        @NotNull @JsonAlias("newPrice") BigDecimal price,
        @NotBlank String currency
) {}
