package com.wolffsoft.jdrivenecommerce.service.product;

import com.wolffsoft.catalog.events.ProductCreatedEvent;
import com.wolffsoft.catalog.events.ProductDeletedEvent;
import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.catalog.events.ProductUpdatedEvent;
import com.wolffsoft.jdrivenecommerce.domain.request.CreateProductRequest;
import com.wolffsoft.jdrivenecommerce.domain.request.UpdatePriceRequest;
import com.wolffsoft.jdrivenecommerce.domain.request.UpdateProductRequest;
import com.wolffsoft.jdrivenecommerce.exception.CurrencyMismatchException;
import com.wolffsoft.jdrivenecommerce.exception.ProductNotFoundException;
import com.wolffsoft.jdrivenecommerce.repository.OutboxEventRepository;
import com.wolffsoft.jdrivenecommerce.repository.ProductRepository;
import com.wolffsoft.jdrivenecommerce.repository.entity.OutboxEventEntity;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.util.JsonUtil;
import com.wolffsoft.jdrivenecommerce.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.wolffsoft.jdrivenecommerce.outbox.OutboxEventStatus.NEW;
import static com.wolffsoft.jdrivenecommerce.util.ProductEventConstants.EVENT_TYPE_PRODUCT_CREATED_V1;
import static com.wolffsoft.jdrivenecommerce.util.ProductEventConstants.EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1;
import static com.wolffsoft.jdrivenecommerce.util.ProductEventConstants.EVENT_TYPE_PRODUCT_UPDATED_V1;
import static com.wolffsoft.jdrivenecommerce.util.ProductEventConstants.EVENT_TYPE_PRODUCT_DELETED_V1;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int EVENT_VERSION = 1;

    private final ProductRepository productRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public ProductEntity create(CreateProductRequest request) {
        ProductEntity product = buildProductEntity(request);

        setPriceUpdateAtIfNull(product);

        ProductEntity savedProduct = productRepository.save(product);
        ProductCreatedEvent productCreatedEvent = buildProductCreatedEvent(savedProduct, request);

        OutboxEventEntity outboxEvent = buildOutboxEvent(
                savedProduct,
                EVENT_TYPE_PRODUCT_CREATED_V1,
                productCreatedEvent
        );
        outboxEventRepository.save(outboxEvent);

        return savedProduct;
    }

    @Transactional
    public ProductEntity update(UUID productId, UpdateProductRequest request) {
        ProductEntity product = getProductOrThrow(productId);

        boolean isChanged = false;

        isChanged |= applyIfNonNull(request.name(), product::setName);
        isChanged |= applyIfNonNull(request.description(), product::setDescription);

        if (request.attributes() != null) {
            product.setAttributes(JsonUtil.toJson(request.attributes()));
            isChanged = true;
        }

        if (!isChanged) {
            return product;
        }

        ProductEntity updatedProduct = productRepository.save(product);

        ProductUpdatedEvent productUpdatedEvent = buildProductUpdatedEvent(updatedProduct, request);

        OutboxEventEntity outboxEvent = buildOutboxEvent(
                updatedProduct,
                EVENT_TYPE_PRODUCT_UPDATED_V1,
                productUpdatedEvent);
        outboxEventRepository.save(outboxEvent);

        return updatedProduct;
    }

    @Transactional
    public ProductEntity updatePrice(UUID productId, UpdatePriceRequest request) {
        ProductEntity product = getProductOrThrow(productId);

        if (!product.getCurrency().equals(request.currency())) {
            throw new CurrencyMismatchException(String.format("Currency mismatch for product with id [%s]",
                    product.getId()));
        }

        long oldPriceCents = product.getPriceInCents();
        long newPriceCents = MoneyUtil.toCents(request.price());

        boolean isEqualOldPriceNewPrice = isEqualOldPriceNewPrice(oldPriceCents, newPriceCents);
        if (isEqualOldPriceNewPrice) {
            return product;
        }

        ProductEntity updatedPrice = updateProduct(product, request);

        ProductPriceUpdatedEvent productPriceUpdatedEvent = buildProductPriceUpdatedEvent(
                updatedPrice, oldPriceCents, newPriceCents);

        OutboxEventEntity outboxEvent = buildOutboxEvent(
                updatedPrice,
                EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1,
                productPriceUpdatedEvent
        );

        outboxEventRepository.save(outboxEvent);

        return updatedPrice;
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        ProductEntity product = getProductOrThrow(productId);

        productRepository.delete(product);

        ProductDeletedEvent productDeletedEvent = buildProductDeletedEvent(product);

        OutboxEventEntity outboxEvent = buildOutboxEvent(
                product,
                EVENT_TYPE_PRODUCT_DELETED_V1,
                productDeletedEvent
        );

        outboxEventRepository.save(outboxEvent);
    }

    public ProductEntity getProductOrThrow(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        String.format("Product not found with id [%s]", productId)
                ));
    }

    private void setPriceUpdateAtIfNull(ProductEntity product) {
        if (product.getPriceUpdatedAt() == null) {
            product.setPriceUpdatedAt(Instant.now());
        }
    }

    private ProductEntity buildProductEntity(CreateProductRequest request) {
        long priceCents = MoneyUtil.toCents(request.price());

        return new ProductEntity(
                request.name(),
                request.description(),
                priceCents,
                request.currency(),
                JsonUtil.toJson(request.attributes() == null ? Map.of() : request.attributes())
        );
    }

    private ProductCreatedEvent buildProductCreatedEvent(ProductEntity savedProduct, CreateProductRequest request) {
        return new ProductCreatedEvent(
                UUID.randomUUID().toString(),
                EVENT_VERSION,
                Instant.now(),
                savedProduct.getId().toString(),
                savedProduct.getName(),
                savedProduct.getDescription() == null ? "" : savedProduct.getDescription(),
                savedProduct.getPriceInCents(),
                savedProduct.getCurrency(),
                request.attributes() == null ? Map.of() : request.attributes()
        );
    }

    private ProductUpdatedEvent buildProductUpdatedEvent(ProductEntity updatedProduct, UpdateProductRequest request) {
        return new ProductUpdatedEvent(
                UUID.randomUUID().toString(),
                EVENT_VERSION,
                Instant.now(),
                updatedProduct.getId().toString(),
                request.name(),
                request.description(),
                request.attributes()
        );
    }

    private ProductPriceUpdatedEvent buildProductPriceUpdatedEvent(
            ProductEntity updatedPrice,
            long oldPriceCents,
            long newPriceCents) {
        return new ProductPriceUpdatedEvent(
                UUID.randomUUID().toString(),
                EVENT_VERSION,
                Instant.now(),
                updatedPrice.getId().toString(),
                oldPriceCents,
                newPriceCents,
                updatedPrice.getCurrency()
        );
    }

    private ProductDeletedEvent buildProductDeletedEvent(ProductEntity product) {
        return new ProductDeletedEvent(
                UUID.randomUUID().toString(),
                EVENT_VERSION,
                Instant.now(),
                product.getId().toString()
        );
    }

    private static <T> boolean applyIfNonNull(T value, java.util.function.Consumer<T> setter) {
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private boolean isEqualOldPriceNewPrice(long oldPriceCents, long newPriceCents) {
        return oldPriceCents == newPriceCents;
    }

    private ProductEntity updateProduct(ProductEntity product, UpdatePriceRequest request) {
        product.setPriceInCents(MoneyUtil.toCents(request.price()));
        product.setPriceUpdatedAt(Instant.now());
        return productRepository.save(product);
    }

    private OutboxEventEntity buildOutboxEvent(ProductEntity product, String eventType, SpecificRecord event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateId(product.getId().toString());
        entity.setEventType(eventType);
        entity.setStatus(NEW);
        entity.setPayload(JsonUtil.toAvroJson(event));
        entity.setCreatedAt(Instant.now());
        entity.setNextAttemptAt(Instant.now());
        entity.setPublishAttempts(0);

        return entity;
    }
}
