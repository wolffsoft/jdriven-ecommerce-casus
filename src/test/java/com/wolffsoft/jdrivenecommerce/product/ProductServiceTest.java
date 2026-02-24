package com.wolffsoft.jdrivenecommerce.product;

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
import com.wolffsoft.jdrivenecommerce.service.product.ProductService;
import com.wolffsoft.jdrivenecommerce.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.wolffsoft.jdrivenecommerce.util.ProductEventConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<ProductEntity> productCaptor;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> outboxCaptor;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("create: persists product and writes a ProductCreatedEvent to the outbox")
    void createPersistsProductAndWritesOutboxEvent() {
        CreateProductRequest request = new CreateProductRequest(
                "Coffee",
                "Nice beans",
                new BigDecimal("12.34"),
                "EUR",
                Map.of("origin", "Ethiopia")
        );

        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        ProductEntity saved = productService.create(request);

        assertThat(saved.getId()).isEqualTo(productId);
        assertThat(saved.getName()).isEqualTo("Coffee");
        assertThat(saved.getDescription()).isEqualTo("Nice beans");
        assertThat(saved.getPriceInCents()).isEqualTo(1234L);
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getPriceUpdatedAt()).isNotNull();

        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getPriceUpdatedAt()).isNotNull();

        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEventEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getId()).isNotNull();
        assertThat(outbox.getAggregateId()).isEqualTo(productId.toString());
        assertThat(outbox.getEventType()).isEqualTo(EVENT_TYPE_PRODUCT_CREATED_V1);
        assertThat(outbox.getPublishAttempts()).isZero();

        ProductCreatedEvent event = JsonUtil.fromAvroJson(outbox.getPayload(), ProductCreatedEvent.class);
        assertThat(event.getProductId()).isEqualTo(productId.toString());
        assertThat(event.getName()).isEqualTo("Coffee");
        assertThat(event.getDescription()).isEqualTo("Nice beans");
        assertThat(event.getPriceInCents()).isEqualTo(1234L);
        assertThat(event.getCurrency()).isEqualTo("EUR");
        assertThat(event.getAttributes()).containsEntry("origin", "Ethiopia");
    }

    @Test
    @DisplayName("update: when request contains no changes, does not touch persistence or outbox")
    void updateWhenNoChangesDoesNotSaveOrWriteOutbox() {
        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        ProductEntity result = productService.update(productId, new UpdateProductRequest(null, null, null));

        assertThat(result).isSameAs(existing);
        verify(productRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("update: persists updated fields and writes a ProductUpdatedEvent")
    void updateWhenFieldsChangedSavesAndWritesOutbox() {
        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProductRequest request = new UpdateProductRequest(
                "Coffee 2",
                "Even nicer",
                Map.of("roast", "dark")
        );

        ProductEntity updated = productService.update(productId, request);

        assertThat(updated.getName()).isEqualTo("Coffee 2");
        assertThat(updated.getDescription()).isEqualTo("Even nicer");

        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getId()).isEqualTo(productId);

        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEventEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getEventType()).isEqualTo(EVENT_TYPE_PRODUCT_UPDATED_V1);

        ProductUpdatedEvent event = JsonUtil.fromAvroJson(outbox.getPayload(), ProductUpdatedEvent.class);
        assertThat(event.getProductId()).isEqualTo(productId.toString());
        assertThat(event.getName()).isEqualTo("Coffee 2");
        assertThat(event.getDescription()).isEqualTo("Even nicer");
        assertThat(event.getAttributes()).containsEntry("roast", "dark");
    }

    @Test
    @DisplayName("update: missing product throws ProductNotFoundException and does not touch persistence or outbox")
    void updateWhenProductMissingThrows() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(productId, new UpdateProductRequest("x", null, null)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePrice: currency mismatch fails fast and does not persist")
    void updatePriceWhenCurrencyMismatchThrowsAndDoesNotPersist() {
        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        UpdatePriceRequest request = new UpdatePriceRequest(new BigDecimal("12.34"), "USD");

        assertThatThrownBy(() -> productService.updatePrice(productId, request))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(productRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePrice: same price is a no-op")
    void updatePriceWhenSamePriceIsNoOp() {
        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        UpdatePriceRequest request = new UpdatePriceRequest(new BigDecimal("12.34"), "EUR");

        ProductEntity result = productService.updatePrice(productId, request);

        assertThat(result).isSameAs(existing);
        verify(productRepository, never()).save(any());
        verify(outboxEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePrice: persists new price, updates timestamp and writes a ProductPriceUpdatedEvent")
    void updatePriceWhenDifferentPricePersistsAndWritesOutbox() {
        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);
        existing.setPriceUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdatePriceRequest request = new UpdatePriceRequest(new BigDecimal("13.37"), "EUR");

        ProductEntity result = productService.updatePrice(productId, request);

        assertThat(result.getPriceInCents()).isEqualTo(1337L);
        assertThat(result.getPriceUpdatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));

        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getPriceInCents()).isEqualTo(1337L);

        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEventEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getEventType()).isEqualTo(EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1);

        ProductPriceUpdatedEvent event = JsonUtil.fromAvroJson(outbox.getPayload(), ProductPriceUpdatedEvent.class);
        assertThat(event.getProductId()).isEqualTo(productId.toString());
        assertThat(event.getOldPriceInCents()).isEqualTo(1234L);
        assertThat(event.getNewPriceInCents()).isEqualTo(1337L);
        assertThat(event.getCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("deleteProduct: deletes product and writes a ProductDeletedEvent to the outbox")
    void deleteProductDeletesAndWritesOutbox() {
        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        productService.deleteProduct(productId);

        verify(productRepository).delete(existing);
        verify(outboxEventRepository).save(outboxCaptor.capture());

        OutboxEventEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getAggregateId()).isEqualTo(productId.toString());
        assertThat(outbox.getEventType()).isEqualTo(EVENT_TYPE_PRODUCT_DELETED_V1);

        ProductDeletedEvent event = JsonUtil.fromAvroJson(outbox.getPayload(), ProductDeletedEvent.class);
        assertThat(event.getProductId()).isEqualTo(productId.toString());
    }

    @Test
    @DisplayName("getProductOrThrow: missing product results in ProductNotFoundException")
    void getProductOrThrowWhenMissingThrows() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductOrThrow(productId))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private ProductEntity withId(ProductEntity p) {
        p.setId(productId);
        return p;
    }
}
