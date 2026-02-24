package com.wolffsoft.jdrivenecommerce.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.wolffsoft.jdrivenecommerce.domain.response.CursorPageResponse;
import com.wolffsoft.jdrivenecommerce.domain.response.ProductSearchResponse;
import com.wolffsoft.jdrivenecommerce.elasticsearch.ProductSearchDocument;
import com.wolffsoft.jdrivenecommerce.exception.ElasticSearchFailedSearchException;
import com.wolffsoft.jdrivenecommerce.util.CursorCodec;
import com.wolffsoft.jdrivenecommerce.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final int SAFE_SIZE_MIN = 1;
    private static final int SAFE_SIZE_MAX = 100;

    private final ElasticsearchClient elasticsearchClient;
    private final CursorCodec cursorCodec;

    @Value("${app.search.index.name}")
    private String indexName;

    public CursorPageResponse<ProductSearchResponse> search(String trimmedQuery, int size, String cursor) {
        int safeSize = getSafeSize(size);

        if (StringUtils.isBlank(trimmedQuery)) {
            return new CursorPageResponse<>(Collections.emptyList(), safeSize, null);
        }

        try {
            SearchResponse<ProductSearchDocument> searchResponse =
                    buildSearchResponse(indexName, safeSize, trimmedQuery, cursor);

            List<Hit<ProductSearchDocument>> hits = searchResponse.hits().hits();

            List<ProductSearchResponse> items = hits.stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .map(ProductSearchService::toResponse)
                    .toList();

            String nextCursor = nextCursorFrom(hits);

            return new CursorPageResponse<>(items, safeSize, nextCursor);

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ElasticSearchFailedSearchException("ElasticSearch failed search", ex);
        }
    }

    private int getSafeSize(int size) {
        return Math.max(SAFE_SIZE_MIN, Math.min(size, SAFE_SIZE_MAX));
    }

    private SearchResponse<ProductSearchDocument> buildSearchResponse(
            String indexName,
            int safeSize,
            String trimmedQuery,
            String cursor
    ) {
        List<FieldValue> searchAfter = StringUtils.isBlank(cursor)
                ? List.of()
                : cursorCodec.decodeCursorToSearchAfter(cursor);

        try {
            return elasticsearchClient.search(searchRequestBuilder -> {
                        SearchRequest.Builder requestBuilder = searchRequestBuilder
                                .index(indexName)
                                .size(safeSize)
                                .sort(scoreDesc())
                                .sort(idAsc())
                                .query(qry -> qry.multiMatch(mm -> mm
                                        .query(trimmedQuery)
                                        .fields("name^4", "description^2", "attributesText", "all")
                                        .fuzziness("AUTO")
                                ));

                        if (!searchAfter.isEmpty()) {
                            requestBuilder = requestBuilder.searchAfter(searchAfter);
                        }

                        return requestBuilder;
                    },
                    ProductSearchDocument.class);

        } catch (IOException ex) {
            throw new ElasticSearchFailedSearchException("ElasticSearch search failed", ex);
        }
    }

    private static ProductSearchResponse toResponse(ProductSearchDocument doc) {
        return new ProductSearchResponse(
                doc.id(),
                doc.name(),
                doc.description(),
                MoneyUtil.fromCents(doc.priceInCents()),
                doc.currency(),
                doc.attributes() == null ? Map.of() : doc.attributes()
        );
    }

    private static SortOptions scoreDesc() {
        return SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc)));
    }

    private static SortOptions idAsc() {
        return SortOptions.of(s -> s.field(f -> f.field("id").order(SortOrder.Asc)));
    }

    private String nextCursorFrom(List<Hit<ProductSearchDocument>> hits) {
        if (hits.isEmpty()) {
            return null;
        }

        List<FieldValue> sort = hits.getLast().sort();
        if (sort == null || sort.isEmpty()) {
            return null;
        }

        return cursorCodec.encodeCursorFromSort(sort);
    }
}
