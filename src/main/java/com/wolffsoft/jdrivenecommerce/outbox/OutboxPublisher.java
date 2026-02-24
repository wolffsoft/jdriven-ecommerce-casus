package com.wolffsoft.jdrivenecommerce.outbox;

import com.wolffsoft.jdrivenecommerce.repository.OutboxEventRepository;
import com.wolffsoft.jdrivenecommerce.repository.entity.OutboxEventEntity;
import com.wolffsoft.jdrivenecommerce.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventTypeRegistry outboxEventTypeRegistry;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String instanceId;

    private final int batchSize;
    private final long staleLockMaxAgeSeconds;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            OutboxEventTypeRegistry outboxEventTypeRegistry,
            KafkaTemplate<String, Object> kafkaTemplate,
            String outboxInstanceId,
            @Value("${outbox.publisher.batch-size}") int batchSize,
            @Value("${outbox.publisher.stale-lock-max-age-seconds}") long staleLockMaxAgeSeconds
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventTypeRegistry = outboxEventTypeRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.instanceId = outboxInstanceId;
        this.batchSize = batchSize;
        this.staleLockMaxAgeSeconds = staleLockMaxAgeSeconds;
    }


    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:500}")
    @Transactional
    public void publishOnce() {
        // Recover stale locks (process crash mid-batch)
        outboxEventRepository.recoverStaleLocks(staleLockMaxAgeSeconds);

        List<UUID> ids = outboxEventRepository.findReadyIds(batchSize);
        if (ids.isEmpty()) {
            return;
        }

        outboxEventRepository.claimIds(ids, instanceId, Instant.now());

        // Read what we actually claimed (if there was a race, this list is smaller)
        List<OutboxEventEntity> claimed = outboxEventRepository.findClaimed(instanceId);
        for (OutboxEventEntity outboxEvent : claimed) {
            publishSingle(outboxEvent);
        }
    }

    private void publishSingle(OutboxEventEntity outboxEventEntity) {
        int attempts = outboxEventEntity.getPublishAttempts() + 1;

        try {
            Class<? extends SpecificRecord> clazz =
                    outboxEventTypeRegistry.eventClass(outboxEventEntity.getEventType());
            SpecificRecord record = JsonUtil.fromAvroJson(outboxEventEntity.getPayload(), clazz);

            String topic = outboxEventTypeRegistry.topic(outboxEventEntity.getEventType());

            kafkaTemplate.send(topic, outboxEventEntity.getAggregateId(), record).get();

            outboxEventRepository.markPublished(outboxEventEntity.getId(), attempts, Instant.now());
        } catch (Exception ex) {
            Instant nextAttempt = Instant.now().plus(OutboxBackoff.computeDelay(attempts));

            String msg = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            if (msg.length() > 3900) msg = msg.substring(0, 3900);

            outboxEventRepository.markFailed(outboxEventEntity.getId(), attempts, nextAttempt, msg);
        }
    }
}
