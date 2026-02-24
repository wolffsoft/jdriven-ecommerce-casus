package com.wolffsoft.jdrivenecommerce.integration;

import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.jdrivenecommerce.domain.request.PriceSyncRequest;
import com.wolffsoft.jdrivenecommerce.exception.CurrencyMismatchException;
import com.wolffsoft.jdrivenecommerce.exception.ProductNotFoundException;
import com.wolffsoft.jdrivenecommerce.repository.OutboxEventRepository;
import com.wolffsoft.jdrivenecommerce.repository.PriceUpdateInboxRepository;
import com.wolffsoft.jdrivenecommerce.repository.ProductRepository;
import com.wolffsoft.jdrivenecommerce.repository.entity.OutboxEventEntity;
import com.wolffsoft.jdrivenecommerce.repository.entity.PriceUpdateInboxEntity;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.service.integration.PriceSyncService;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.wolffsoft.jdrivenecommerce.util.ProductEventConstants.EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceSyncServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PriceUpdateInboxRepository inboxRepository;

    @Mock
    private OutboxEventRepository outboxRepository;

    @InjectMocks
    private PriceSyncService priceSyncService;

    @Captor
    private ArgumentCaptor<OutboxEventEntity> outboxCaptor;

    @Captor
    private ArgumentCaptor<PriceUpdateInboxEntity> inboxCaptor;

    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
    }

    @Test
    @DisplayName("syncPrice: duplicate requestId is idempotent and returns current product without updating")
    void syncPriceWhenDuplicateInboxRequestReturnsCurrentProductWithoutUpdating() {
        PriceSyncRequest request = new PriceSyncRequest(
                "req-1",
                productId,
                2000L,
                "EUR",
                Instant.parse("2026-02-01T10:00:00Z"),
                "catalog"
        );

        when(inboxRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        ProductEntity result = priceSyncService.syncPrice(request);

        assertThat(result).isSameAs(existing);

        verify(inboxRepository).save(any());
        verify(productRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncPrice: throws ProductNotFoundException when product does not exist")
    void syncPriceWhenProductMissingThrows() {
        PriceSyncRequest request = new PriceSyncRequest(
                "req-1",
                productId,
                2000L,
                "EUR",
                Instant.parse("2026-02-01T10:00:00Z"),
                "catalog"
        );

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceSyncService.syncPrice(request))
                .isInstanceOf(ProductNotFoundException.class);

        verify(outboxRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncPrice: currency mismatch fails fast and does not update product or outbox")
    void syncPriceWhenCurrencyMismatchThrows() {
        PriceSyncRequest request = new PriceSyncRequest(
                "req-1",
                productId,
                2000L,
                "USD",
                Instant.parse("2026-02-01T10:00:00Z"),
                "catalog"
        );

        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> priceSyncService.syncPrice(request))
                .isInstanceOf(CurrencyMismatchException.class);

        verify(productRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncPrice: ignores stale updates when effectiveAt is older than current priceUpdatedAt")
    void syncPriceWhenEffectiveBeforeCurrentReturnsProductWithoutUpdating() {
        Instant current = Instant.parse("2026-02-10T10:00:00Z");

        PriceSyncRequest request = new PriceSyncRequest(
                "req-1",
                productId,
                2000L,
                "EUR",
                Instant.parse("2026-02-01T10:00:00Z"),
                "catalog"
        );

        when(inboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);
        existing.setPriceUpdatedAt(current);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        ProductEntity result = priceSyncService.syncPrice(request);

        assertThat(result).isSameAs(existing);

        verify(productRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncPrice: no-op when price is identical and effectiveAt is not advanced")
    void syncPriceWhenNoPriceChangeAndEffectiveNotAdvancedReturnsProductWithoutUpdating() {
        Instant current = Instant.parse("2026-02-01T10:00:00Z");

        PriceSyncRequest request = new PriceSyncRequest(
                "req-1",
                productId,
                1234L,
                "EUR",
                current,
                "catalog"
        );

        when(inboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);
        existing.setPriceUpdatedAt(current);
        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));

        ProductEntity result = priceSyncService.syncPrice(request);

        assertThat(result).isSameAs(existing);

        verify(productRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncPrice: advances priceUpdatedAt when effectiveAt is newer even if price is unchanged, without emitting outbox")
    void syncPriceWhenEffectiveAdvancedButSamePriceUpdatesTimestampWithoutOutbox() {
        Instant current = Instant.parse("2026-02-01T10:00:00Z");
        Instant advanced = Instant.parse("2026-02-02T10:00:00Z");

        PriceSyncRequest request = new PriceSyncRequest(
                "req-1",
                productId,
                1234L,
                "EUR",
                advanced,
                "catalog"
        );

        when(inboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);
        existing.setPriceUpdatedAt(current);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity result = priceSyncService.syncPrice(request);

        assertThat(result.getPriceInCents()).isEqualTo(1234L);
        assertThat(result.getPriceUpdatedAt()).isEqualTo(advanced);

        verify(productRepository).save(any(ProductEntity.class));
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("syncPrice: when price changes, persists new price and emits ProductPriceUpdatedEvent")
    void syncPriceWhenPriceChangesUpdatesAndWritesOutbox() {
        Instant current = Instant.parse("2026-02-01T10:00:00Z");
        Instant advanced = Instant.parse("2026-02-02T10:00:00Z");

        PriceSyncRequest request = new PriceSyncRequest(
                "req-1",
                productId,
                2000L,
                "EUR",
                advanced,
                "catalog"
        );

        when(inboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity existing = new ProductEntity("Coffee", "Nice", 1234L, "EUR", "{}");
        existing.setId(productId);
        existing.setPriceUpdatedAt(current);

        when(productRepository.findById(productId)).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductEntity result = priceSyncService.syncPrice(request);

        assertThat(result.getPriceInCents()).isEqualTo(2000L);
        assertThat(result.getPriceUpdatedAt()).isEqualTo(advanced);

        verify(inboxRepository).save(inboxCaptor.capture());
        assertThat(inboxCaptor.getValue().getRequestId()).isEqualTo("req-1");

        verify(outboxRepository).save(outboxCaptor.capture());
        OutboxEventEntity outbox = outboxCaptor.getValue();
        assertThat(outbox.getEventType()).isEqualTo(EVENT_TYPE_PRODUCT_PRICE_UPDATED_V1);

        ProductPriceUpdatedEvent event = JsonUtil.fromAvroJson(outbox.getPayload(), ProductPriceUpdatedEvent.class);
        assertThat(event.getProductId()).isEqualTo(productId.toString());
        assertThat(event.getOldPriceInCents()).isEqualTo(1234L);
        assertThat(event.getNewPriceInCents()).isEqualTo(2000L);
        assertThat(event.getCurrency()).isEqualTo("EUR");
    }
}
