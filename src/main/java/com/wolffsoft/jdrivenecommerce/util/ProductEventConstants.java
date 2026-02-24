package com.wolffsoft.jdrivenecommerce.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProductEventConstants {

    public static final String EVENT_TYPE_PRODUCT_CREATED_V1 = "product.created.v1";
    public static final String EVENT_TYPE_PRODUCT_UPDATED_V1 = "product.updated.v1";
    public static final String EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1 = "product.price-updated.v1";
    public static final String EVENT_TYPE_PRODUCT_DELETED_V1 = "product.deleted.v1";
}
