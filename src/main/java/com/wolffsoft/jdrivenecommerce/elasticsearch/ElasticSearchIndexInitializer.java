package com.wolffsoft.jdrivenecommerce.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.DynamicMapping;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import com.wolffsoft.jdrivenecommerce.exception.ElasticSearchIndicesExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchIndexInitializer implements ApplicationRunner {

    @Value("${app.search.index.name}")
    private String indexName;

    @Value("${app.search.index.init.maxAttempts:10}")
    private int maxAttempts;

    @Value("${app.search.index.init.delayMs:1000}")
    private long delayMs;

    private final ElasticsearchClient elasticsearchClient;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        initializeIndex();
    }

    public void initializeIndex() {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (indexExists()) {
                    log.info("Elasticsearch index [{}] already exists.", indexName);
                    return;
                }

                createIndex();
                log.info("Elasticsearch index [{}] created successfully.", indexName);
                return;

            } catch (IOException ex) {
                if (attempt == maxAttempts) {
                    throw new ElasticSearchIndicesExistsException(
                            String.format("Failed to initialize Elasticsearch index [%s] after [%s] attempts",
                                    indexName, maxAttempts), ex);
                }

                log.warn("Attempt {}/{} to initialize Elasticsearch index [{}] failed: {}",
                        attempt, maxAttempts, indexName, ex.getMessage());

                sleep(delayMs);
            }
        }
    }

    private boolean indexExists() throws IOException {
        return elasticsearchClient.indices()
                .exists(req -> req.index(indexName))
                .value();
    }

    private void createIndex() throws IOException {
        log.info("Creating ElasticSearch index [{}]", indexName);
        elasticsearchClient.indices().create(create ->
                create.index(indexName)
                        .settings(setting -> setting
                                .numberOfShards("1")
                                .numberOfReplicas("1"))
                        .mappings(this::createBuilder)
        );
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to retry Elasticsearch initialization", ex);
        }
    }

    private TypeMapping.Builder createBuilder(TypeMapping.Builder mapping) {
        return mapping
                .properties("id", p -> p.keyword(k -> k))
                .properties("name", p -> p.text(t -> t.copyTo("all")))
                .properties("description", p -> p.text(t -> t.copyTo("all")))
                .properties("attributesText", p -> p.text(t -> t.copyTo("all")))
                .properties("currencyText", p -> p.text(t -> t.copyTo("all")))
                .properties("priceText", p -> p.text(t -> t.copyTo("all")))
                .properties("priceInCents", p -> p.long_(l -> l))
                .properties("currency", p -> p.keyword(k -> k))
                .properties("attributes", p -> p.object(o -> o.dynamic(DynamicMapping.True)))
                .properties("all", p -> p.text(t -> t));
    }
}
