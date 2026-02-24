package com.wolffsoft.jdrivenecommerce.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.wolffsoft.catalog.events.ProductCreatedEvent;
import com.wolffsoft.catalog.events.ProductPriceUpdatedEvent;
import com.wolffsoft.catalog.events.ProductUpdatedEvent;
import com.wolffsoft.jdrivenecommerce.elasticsearch.ProductSearchDocument;
import com.wolffsoft.jdrivenecommerce.exception.ElasticSearchFailedUpdateException;
import com.wolffsoft.jdrivenecommerce.exception.ElasticSearchFailedUpsertException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticsearchSearchProjectionService implements SearchProjectionService {

    @Value("${app.search.index.name}")
    private String indexName;

    private static final boolean DOC_AS_UPSERT = Boolean.TRUE;

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void upsertProduct(ProductCreatedEvent event) {
        try {
            Map<String, String> attributes = event.getAttributes() == null ? Map.of() : event.getAttributes();

            String currencyText = event.getCurrency() == null ? "" : event.getCurrency();
            String priceText = buildPriceText(event.getPriceInCents(), event.getCurrency());

            ProductSearchDocument doc = new ProductSearchDocument(
                    event.getProductId(),
                    event.getName(),
                    event.getDescription(),
                    event.getPriceInCents(),
                    event.getCurrency(),
                    currencyText,
                    priceText,
                    attributes,
                    buildAttributesText(attributes)
            );

            elasticsearchClient.index(i -> i.index(indexName).id(doc.id()).document(doc));
        } catch (IOException ex) {
            throw new ElasticSearchFailedUpsertException(
                    String.format("Failed to upsert product in Elasticsearch with product id [%s]", event.getProductId()),
                    ex);
        }
    }

    @Override
    public void partialUpdateProduct(ProductUpdatedEvent event) {
        try {
            Map<String, Object> updateProduct = buildUpdateProduct(event);
            if (updateProduct.isEmpty()) {
                return;
            }

            elasticsearchClient.update(update -> update
                            .index(indexName)
                            .id(event.getProductId())
                            .doc(updateProduct)
                            .docAsUpsert(DOC_AS_UPSERT),
                    Object.class);
        } catch (IOException ex) {
            throw new ElasticSearchFailedUpdateException(
                    String.format("Failed to update product in ElasticSearch with product id [%s]", event.getProductId()),
                    ex);
        }
    }

    @Override
    public void buildUpdatePrice(ProductPriceUpdatedEvent event) {
        try {
            Map<String, Object> updatePrice = createUpdateProductPrice(event);

            elasticsearchClient.update(update -> update
                            .index(indexName)
                            .id(event.getProductId())
                            .doc(updatePrice)
                            .docAsUpsert(DOC_AS_UPSERT),
                    Object.class);
        } catch (IOException ex) {
            throw new ElasticSearchFailedUpsertException(
                    String.format("Failed to update price in ElasticSearch with product id [%s]", event.getProductId()),
                    ex);
        }
    }

    @Override
    public void deleteProduct(String productId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(indexName)
                    .id(productId));
        } catch (IOException ex) {
            throw new ElasticSearchFailedUpdateException(
                    String.format("Failed to delete product in ElasticSearch with product id [%s]", productId),
                    ex
            );
        }
    }

    private String buildAttributesText(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "";
        }
        return attributes.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(" "));
    }

    private Map<String, Object> buildUpdateProduct(ProductUpdatedEvent event) {
        Map<String, Object> updateProduct = new HashMap<>();
        Optional<ProductUpdatedEvent> optionalEvent = Optional.ofNullable(event);

        optionalEvent.map(ProductUpdatedEvent::getName).ifPresent(name -> updateProduct.put("name", name));
        optionalEvent.map(ProductUpdatedEvent::getDescription)
                .ifPresent(description -> updateProduct.put("description", description));
        optionalEvent.map(ProductUpdatedEvent::getAttributes)
                .ifPresent(attributes -> {
                    updateProduct.put("attributes", attributes);
                    updateProduct.put("attributesText", buildAttributesText(attributes));
                });

        return updateProduct;
    }

    private Map<String, Object> createUpdateProductPrice(ProductPriceUpdatedEvent event) {
        return Map.of(
                "priceInCents", event.getNewPriceInCents(),
                "currency", event.getCurrency(),
                "currencyText", event.getCurrency() == null ? "" : event.getCurrency(),
                "priceText", buildPriceText(event.getNewPriceInCents(), event.getCurrency())
        );
    }

    private static String buildPriceText(Long priceInCents, String currency) {
        if (priceInCents == null) {
            return currency == null ? "" : currency;
        }

        // Provide a few text tokens so single-field search can match "1999", "19.99", and currency.
        BigDecimal major = BigDecimal.valueOf(priceInCents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
        String centsToken = String.valueOf(priceInCents);
        String majorToken = major.toPlainString();
        String currencyToken = currency == null ? "" : currency;

        return (centsToken + " " + majorToken + " " + currencyToken).trim();
    }
}
