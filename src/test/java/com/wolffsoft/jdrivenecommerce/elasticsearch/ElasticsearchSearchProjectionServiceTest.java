package com.wolffsoft.jdrivenecommerce.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.UpdateRequest;
import co.elastic.clients.elasticsearch.core.UpdateResponse;
import co.elastic.clients.util.ObjectBuilder;
import com.wolffsoft.catalog.events.ProductCreatedEvent;
import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.catalog.events.ProductUpdatedEvent;
import com.wolffsoft.jdrivenecommerce.exception.ElasticSearchFailedUpdateException;
import com.wolffsoft.jdrivenecommerce.exception.ElasticSearchFailedUpsertException;
import com.wolffsoft.jdrivenecommerce.service.elasticsearch.ElasticsearchSearchProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElasticsearchSearchProjectionServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @InjectMocks
    private ElasticsearchSearchProjectionService service;

    @Captor
    private ArgumentCaptor<Function> fnCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "indexName", "products");
    }

    @Test
    @DisplayName("upsertProduct: indexes a denormalized ProductSearchDocument with attributesText")
    void upsertProductIndexesDocumentWithAttributesText() throws Exception {
        String productId = UUID.randomUUID().toString();

        ProductCreatedEvent event = new ProductCreatedEvent(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                productId,
                "Coffee",
                "Nice",
                1234L,
                "EUR",
                Map.of("origin", "Ethiopia")
        );

        when(elasticsearchClient.index(any(Function.class))).thenReturn(mock(IndexResponse.class));

        service.upsertProduct(event);

        verify(elasticsearchClient).index(fnCaptor.capture());

        @SuppressWarnings("unchecked")
        Function<IndexRequest.Builder<?>, ObjectBuilder<IndexRequest<?>>> fn = fnCaptor.getValue();
        IndexRequest<?> request = fn.apply(new IndexRequest.Builder<>()).build();

        assertThat(request.index()).isEqualTo("products");
        assertThat(request.id()).isEqualTo(productId);
        assertThat(request.document()).isInstanceOf(ProductSearchDocument.class);

        ProductSearchDocument doc = (ProductSearchDocument) request.document();
        assertThat(doc.id()).isEqualTo(productId);
        assertThat(doc.attributes()).containsEntry("origin", "Ethiopia");
        assertThat(doc.attributesText()).contains("origin").contains("Ethiopia");
    }

    @Test
    @DisplayName("upsertProduct: wraps IOExceptions in ElasticSearchFailedUpsertException")
    void upsertProductWhenIOExceptionWrapsInDomainException() throws Exception {
        ProductCreatedEvent event = new ProductCreatedEvent(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                UUID.randomUUID().toString(),
                "Coffee",
                "Nice",
                1234L,
                "EUR",
                Map.of()
        );

        when(elasticsearchClient.index(any(Function.class))).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.upsertProduct(event))
                .isInstanceOf(ElasticSearchFailedUpsertException.class);
    }

    @Test
    @DisplayName("partialUpdateProduct: does not call Elasticsearch when there is nothing to update")
    void partialUpdateProductWhenNoFieldsDoesNothing() throws Exception {
        ProductUpdatedEvent event = new ProductUpdatedEvent(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                UUID.randomUUID().toString(),
                null,
                null,
                null
        );

        service.partialUpdateProduct(event);

        verify(elasticsearchClient, never()).update(any(Function.class), any());
    }

    @Test
    @DisplayName("partialUpdateProduct: sends update with docAsUpsert=true and derived attributesText")
    void partialUpdateProductWhenFieldsPresentUpdatesWithDocAsUpsert() throws Exception {
        String productId = UUID.randomUUID().toString();

        ProductUpdatedEvent event = new ProductUpdatedEvent(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                productId,
                "Coffee 2",
                null,
                Map.of("roast", "dark")
        );

        when(elasticsearchClient.update(any(Function.class), eq(Object.class))).thenReturn(mock(UpdateResponse.class));

        service.partialUpdateProduct(event);

        verify(elasticsearchClient).update(fnCaptor.capture(), eq(Object.class));

        @SuppressWarnings("unchecked")
        Function<UpdateRequest.Builder<?, ?>, ObjectBuilder<UpdateRequest<?, ?>>> fn = fnCaptor.getValue();
        UpdateRequest<?, ?> request = fn.apply(new UpdateRequest.Builder<>()).build();

        assertThat(request.index()).isEqualTo("products");
        assertThat(request.id()).isEqualTo(productId);
        assertThat(request.docAsUpsert()).isTrue();
        assertThat(request.doc()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> doc = (Map<String, Object>) request.doc();
        assertThat(doc).containsEntry("name", "Coffee 2");
        assertThat(doc).containsKey("attributes");
        assertThat(doc).containsKey("attributesText");
        assertThat(doc.get("attributesText").toString()).contains("roast").contains("dark");
    }

    @Test
    @DisplayName("partialUpdateProduct: wraps IOExceptions in ElasticSearchFailedUpdateException")
    void partialUpdateProductWhenIOExceptionWrapsInUpdateException() throws Exception {
        ProductUpdatedEvent event = new ProductUpdatedEvent(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                UUID.randomUUID().toString(),
                "Coffee",
                null,
                null
        );

        when(elasticsearchClient.update(any(Function.class), eq(Object.class))).thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.partialUpdateProduct(event))
                .isInstanceOf(ElasticSearchFailedUpdateException.class);
    }

    @Test
    @DisplayName("buildUpdatePrice: updates price and currency with docAsUpsert=true")
    void buildUpdatePriceUpdatesPriceAndCurrency() throws Exception {
        String productId = UUID.randomUUID().toString();

        ProductPriceUpdatedEvent event = new ProductPriceUpdatedEvent(
                UUID.randomUUID().toString(),
                1,
                Instant.now(),
                productId,
                100L,
                200L,
                "EUR"
        );

        when(elasticsearchClient.update(any(Function.class), eq(Object.class))).thenReturn(mock(UpdateResponse.class));

        service.buildUpdatePrice(event);

        verify(elasticsearchClient).update(fnCaptor.capture(), eq(Object.class));

        @SuppressWarnings("unchecked")
        Function<UpdateRequest.Builder<?, ?>, ObjectBuilder<UpdateRequest<?, ?>>> fn = fnCaptor.getValue();
        UpdateRequest<?, ?> request = fn.apply(new UpdateRequest.Builder<>()).build();

        assertThat(request.index()).isEqualTo("products");
        assertThat(request.id()).isEqualTo(productId);
        assertThat(request.docAsUpsert()).isTrue();

        @SuppressWarnings("unchecked")
        Map<String, Object> doc = (Map<String, Object>) request.doc();
        assertThat(doc).containsEntry("priceInCents", 200L);
        assertThat(doc).containsEntry("currency", "EUR");
    }

    @Test
    @DisplayName("deleteProduct: deletes by id on the configured index")
    void deleteProductDeletesById() throws Exception {
        when(elasticsearchClient.delete(any(Function.class))).thenReturn(mock(DeleteResponse.class));

        service.deleteProduct("p-1");

        verify(elasticsearchClient).delete(fnCaptor.capture());

        @SuppressWarnings("unchecked")
        Function<DeleteRequest.Builder, ObjectBuilder<DeleteRequest>> fn = fnCaptor.getValue();
        DeleteRequest request = fn.apply(new DeleteRequest.Builder()).build();

        assertThat(request.index()).isEqualTo("products");
        assertThat(request.id()).isEqualTo("p-1");
    }
}
