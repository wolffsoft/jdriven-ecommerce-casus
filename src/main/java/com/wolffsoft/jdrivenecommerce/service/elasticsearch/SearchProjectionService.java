package com.wolffsoft.jdrivenecommerce.service.elasticsearch;

import com.wolffsoft.catalog.events.ProductCreatedEvent;
import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.catalog.events.ProductUpdatedEvent;

public interface SearchProjectionService {
    void upsertProduct(ProductCreatedEvent event);
    void partialUpdateProduct(ProductUpdatedEvent event);
    void buildUpdatePrice(ProductPriceUpdatedEvent event);
    void deleteProduct(String productId);
}
