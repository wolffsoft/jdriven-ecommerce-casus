package com.wolffsoft.jdrivenecommerce.kafka.consumer;

import com.wolffsoft.catalog.events.ProductCreatedEvent;
import com.wolffsoft.catalog.events.ProductDeletedEvent;
import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.catalog.events.ProductUpdatedEvent;
import com.wolffsoft.jdrivenecommerce.service.elasticsearch.SearchProjectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        id = "product-events-listener",
        topics = "${app.kafka.topics.product-events}"
)
public class ProductEventsListener {

    private final SearchProjectionService projectionService;

    @KafkaHandler
    public void create(
            ProductCreatedEvent event,
            Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed ProductCreatedEvent productId={} partition={} offset={}",
                event.getProductId(), partition, offset);

        projectionService.upsertProduct(event);

        ack.acknowledge();
    }

    @KafkaHandler
    public void update(
            ProductUpdatedEvent event,
            Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed ProductUpdatedEvent productId={} partition={} offset={}",
                event.getProductId(), partition, offset);

        projectionService.partialUpdateProduct(event);

        ack.acknowledge();
    }

    @KafkaHandler
    public void updatePrice(
            ProductPriceUpdatedEvent event,
            Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed ProductPriceUpdatedEvent productId={} partition={} offset={}",
                event.getProductId(), partition, offset);

        projectionService.buildUpdatePrice(event);

        ack.acknowledge();
    }

    @KafkaHandler
    public void delete(
            ProductDeletedEvent event,
            Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Consumed ProductDeletedEvent productId={} partition={} offset={}",
                event.getProductId(), partition, offset);

        projectionService.deleteProduct(event.getProductId());

        ack.acknowledge();
    }

    /**
     * Default handler: if an unknown event arrives (new schema/type),
     * throw so the message is NOT acknowledged.
     * Kafka will retry depending on the container/error handler settings.
     */
    @KafkaHandler(isDefault = true)
    public void onUnknown(Object unknown) {
        ClassLoader payloadCl = unknown == null ? null : unknown.getClass().getClassLoader();
        ClassLoader expectedCl = ProductPriceUpdatedEvent.class.getClassLoader();

        log.warn("UNKNOWN payloadType={} payloadClassLoader={} expectedPriceUpdateClassLoader={}",
                unknown == null ? "null" : unknown.getClass().getName(),
                payloadCl,
                expectedCl);

        throw new IllegalArgumentException("Unsupported event type: " +
                (unknown == null ? "null" : unknown.getClass().getName()));
    }
}
