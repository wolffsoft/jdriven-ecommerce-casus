package com.wolffsoft.jdrivenecommerce.domain.response;

import java.util.UUID;

public record PriceSyncResponse(
        UUID productId,
        String requestId,
        PriceSyncResult result,
        String message) {
    public enum PriceSyncResult {
        APPLIED,
        DUPLICATE,
        SKIPPED_OUT_OF_ORDER,
        PRODUCT_NOT_FOUND
    }
}
