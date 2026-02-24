package com.wolffsoft.jdrivenecommerce.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventPublisher {

    @Value("${app.kafka.topics.product-events}") String topic;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishImmediately(String key, Object event) {
        Objects.requireNonNull(event, "event must not be null");

        if (!(event instanceof SpecificRecord)) {
            throw new IllegalArgumentException(

                    String.format("Kafka event must be Avro SpecificRecord but was [%s]", event.getClass().getName())
            );
        }

        send(key, event);
    }

    private void send(String key, Object event) {
        try {
            CompletableFuture<?> future = kafkaTemplate.send(topic, key, event);
            future.whenComplete((res, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish Kafka event. topic={} key={} eventType={}",
                            topic, key, event.getClass().getSimpleName(), ex);
                } else {
                    log.debug("Published Kafka event. topic={} key={} eventType={}",
                            topic, key, event.getClass().getSimpleName());
                }
            });
        } catch (Exception ex) {
            log.error("Synchronous failure while publishing Kafka event. topic={} key={} eventType={}",
                    topic, key, event.getClass().getSimpleName(), ex);
        }
    }
}
