package com.wolffsoft.jdrivenecommerce.domain.request;

import java.util.Map;

public record UpdateProductRequest(
        String name,
        String description,
        Map<String, String> attributes
) {}
