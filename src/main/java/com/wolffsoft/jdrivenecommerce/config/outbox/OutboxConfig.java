package com.wolffsoft.jdrivenecommerce.config.outbox;

import com.wolffsoft.catalog.events.ProductCreatedEvent;
import com.wolffsoft.catalog.events.ProductDeletedEvent;
import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.catalog.events.ProductUpdatedEvent;
import com.wolffsoft.jdrivenecommerce.outbox.OutboxEventTypeRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.UUID;

@Configuration
public class OutboxConfig {

    public static final String EVENT_PRODUCT_CREATED_V1 = "product.created.v1";
    public static final String EVENT_PRODUCT_UPDATED_V1 = "product.updated.v1";
    public static final String EVENT_PRODUCT_PRICE_UPDATED_V1 = "product.price-updated.v1";
    public static final String EVENT_PRODUCT_DELETED_V1 = "product.deleted.v1";

    @Bean
    public String outboxInstanceId(@Value("${spring.application.name}") String appName) {
        return appName + "-" + UUID.randomUUID();
    }

    @Bean
    public OutboxEventTypeRegistry outboxEventTypeRegistry(
            @Value("${app.kafka.topics.product-events}") String topicName) {
        return new OutboxEventTypeRegistry(
                Map.of(
                        EVENT_PRODUCT_CREATED_V1, ProductCreatedEvent.class,
                        EVENT_PRODUCT_UPDATED_V1, ProductUpdatedEvent.class,
                        EVENT_PRODUCT_PRICE_UPDATED_V1, ProductPriceUpdatedEvent.class,
                        EVENT_PRODUCT_DELETED_V1, ProductDeletedEvent.class
                ),
                Map.of(
                        EVENT_PRODUCT_CREATED_V1, topicName,
                        EVENT_PRODUCT_UPDATED_V1, topicName,
                        EVENT_PRODUCT_PRICE_UPDATED_V1, topicName,
                        EVENT_PRODUCT_DELETED_V1, topicName
                )
        );
    }
}
