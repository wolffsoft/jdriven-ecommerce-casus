package com.wolffsoft.jdrivenecommerce.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.wolffsoft.jdrivenecommerce.elasticsearch.ProductSearchDocument;
import com.wolffsoft.jdrivenecommerce.repository.ProductRepository;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

import static com.wolffsoft.jdrivenecommerce.util.ElasticSearchUtil.buildPriceText;

@Service
@RequiredArgsConstructor
public class ReIndexService {

    private final ProductRepository productRepository;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${app.search.index:products}")
    private String indexName;

    public ReindexResult reindexAll(int batchSize) {
        int size = Math.max(100, Math.min(batchSize, 2000));
        long totalIndexed = 0;

        int page = 0;
        while (true) {
            var slice = productRepository.findAll(PageRequest.of(page, size));
            if (slice.isEmpty()) break;

            BulkRequest.Builder bulk = new BulkRequest.Builder();

            for (ProductEntity p : slice.getContent()) {
                Map<String, String> attrs = JsonUtil.fromJson(p.getAttributes(), new TypeReference<>() {});

                String currencyText = p.getCurrency() == null ? "" : p.getCurrency();
                String priceText = buildPriceText(p.getPriceInCents(), p.getCurrency());

                ProductSearchDocument doc = new ProductSearchDocument(
                        p.getId().toString(),
                        p.getName(),
                        p.getDescription(),
                        p.getPriceInCents(),
                        p.getCurrency(),
                        currencyText,
                        priceText,
                        attrs,
                        buildAttributesText(attrs)
                );

                bulk.operations(op -> op
                        .index(i -> i
                                .index(indexName)
                                .id(doc.id())
                                .document(doc)
                        )
                );
            }

            try {
                var resp = elasticsearchClient.bulk(bulk.build());
                if (resp.errors()) {
                    throw new IllegalStateException("Bulk indexing had errors");
                }
            } catch (Exception ex) {
                throw new RuntimeException(String.format("Bulk indexing failed on page %s", page), ex);
            }

            totalIndexed += slice.getNumberOfElements();
            page++;
        }

        try {
            elasticsearchClient.indices().refresh(r -> r.index(indexName));
        } catch (Exception ex) {
            throw new RuntimeException(String.format("Failed to refresh index %s", indexName), ex);
        }

        return new ReindexResult(totalIndexed);
    }

    private static String buildAttributesText(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) return "";
        return attributes.entrySet().stream()
                .map(e -> e.getKey() + " " + e.getValue())
                .collect(Collectors.joining(" "));
    }

    public record ReindexResult(long indexedCount) {}
}
