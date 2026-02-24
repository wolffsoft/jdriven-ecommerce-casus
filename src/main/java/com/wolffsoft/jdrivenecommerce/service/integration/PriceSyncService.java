package com.wolffsoft.jdrivenecommerce.service.integration;

import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.jdrivenecommerce.domain.request.PriceSyncRequest;
import com.wolffsoft.jdrivenecommerce.exception.CurrencyMismatchException;
import com.wolffsoft.jdrivenecommerce.exception.ProductNotFoundException;
import com.wolffsoft.jdrivenecommerce.outbox.OutboxEventStatus;
import com.wolffsoft.jdrivenecommerce.repository.OutboxEventRepository;
import com.wolffsoft.jdrivenecommerce.repository.PriceUpdateInboxRepository;
import com.wolffsoft.jdrivenecommerce.repository.ProductRepository;
import com.wolffsoft.jdrivenecommerce.repository.entity.OutboxEventEntity;
import com.wolffsoft.jdrivenecommerce.repository.entity.PriceUpdateInboxEntity;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static com.wolffsoft.jdrivenecommerce.util.ProductEventConstants.EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1;

@Service
@RequiredArgsConstructor
public class PriceSyncService {

    private static final int EVENT_VERSION = 1;

    private final ProductRepository productRepository;
    private final PriceUpdateInboxRepository inboxRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional
    public ProductEntity syncPrice(PriceSyncRequest request) {
        ProductEntity product = getProductOrThrow(request.productId());

        if (!product.getCurrency().equals(request.currency())) {
            throw new CurrencyMismatchException(
                    String.format("Currency mismatch for product with id [%s]", request.productId())
            );
        }

        try {
            inboxRepository.save(createUpdateInboxEntity(request));
        } catch (DataIntegrityViolationException duplicate) {
            return product;
        }

        Instant currentEffective = product.getPriceUpdatedAt();
        if (currentEffective != null && request.effectiveAt().isBefore(currentEffective)) {
            return product;
        }

        long oldPriceCents = product.getPriceInCents();
        long newPriceCents = request.priceInCents();

        boolean hasPriceChanged = oldPriceCents != newPriceCents;
        boolean isEffectiveAdvanced = currentEffective == null || request.effectiveAt().isAfter(currentEffective);

        if (!hasPriceChanged && !isEffectiveAdvanced) {
            return product;
        }

        ProductEntity updated = updateProductPrice(product, request);

        if (hasPriceChanged) {
            ProductPriceUpdatedEvent event = buildPriceUpdatedEvent(updated, oldPriceCents, newPriceCents);
            outboxRepository.save(buildOutbox(
                    updated.getId().toString(),
                    EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1,
                    event
            ));
        }

        return updated;
    }

    private PriceUpdateInboxEntity createUpdateInboxEntity(PriceSyncRequest request) {
        return new PriceUpdateInboxEntity(
                request.requestId(),
                request.productId(),
                request.effectiveAt(),
                Instant.now(),
                request.source()
        );
    }

    private ProductEntity updateProductPrice(ProductEntity product, PriceSyncRequest request) {
        product.setPriceInCents(request.priceInCents());
        product.setPriceUpdatedAt(request.effectiveAt());
        return productRepository.save(product);
    }

    private ProductEntity getProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        String.format("Product not found with id [%s]", productId)
                ));
    }

    private ProductPriceUpdatedEvent buildPriceUpdatedEvent(
            ProductEntity product,
            long oldPriceCents,
            long newPriceCents) {
        return new ProductPriceUpdatedEvent(
                UUID.randomUUID().toString(),
                EVENT_VERSION,
                Instant.now(),
                product.getId().toString(),
                oldPriceCents,
                newPriceCents,
                product.getCurrency()
        );
    }

    private OutboxEventEntity buildOutbox(String aggregateId, String eventType, SpecificRecord event) {
        OutboxEventEntity outboxEventEntity = new OutboxEventEntity();
        outboxEventEntity.setId(UUID.randomUUID());
        outboxEventEntity.setAggregateId(aggregateId);
        outboxEventEntity.setEventType(eventType);
        outboxEventEntity.setPayload(JsonUtil.toAvroJson(event));
        outboxEventEntity.setStatus(OutboxEventStatus.NEW);
        outboxEventEntity.setNextAttemptAt(Instant.now());
        outboxEventEntity.setCreatedAt(Instant.now());
        outboxEventEntity.setPublishAttempts(0);
        return outboxEventEntity;
    }
}
