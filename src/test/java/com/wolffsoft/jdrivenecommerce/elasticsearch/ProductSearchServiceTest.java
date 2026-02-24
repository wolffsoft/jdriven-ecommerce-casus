package com.wolffsoft.jdrivenecommerce.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.ShardStatistics;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.util.ObjectBuilder;
import com.wolffsoft.jdrivenecommerce.domain.response.CursorPageResponse;
import com.wolffsoft.jdrivenecommerce.domain.response.ProductSearchResponse;
import com.wolffsoft.jdrivenecommerce.exception.ElasticSearchFailedSearchException;
import com.wolffsoft.jdrivenecommerce.service.elasticsearch.ProductSearchService;
import com.wolffsoft.jdrivenecommerce.util.CursorCodec;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSearchServiceTest {

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private CursorCodec cursorCodec;

    @InjectMocks
    private ProductSearchService service;

    @Captor
    private ArgumentCaptor<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>> searchFnCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "indexName", "products");
    }

    @Test
    @DisplayName("search: blank query returns an empty page without calling Elasticsearch")
    void searchWhenBlankQueryReturnsEmptyAndDoesNotCallElasticsearch() {
        CursorPageResponse<ProductSearchResponse> page = service.search("  ", 10, null);

        assertThat(page.items()).isEmpty();
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.nextCursor()).isNull();

        verifyNoInteractions(elasticsearchClient);
        verifyNoInteractions(cursorCodec);
    }

    @Test
    @DisplayName("search: clamps page size to a safe range")
    void searchClampsSizeToSafeRange() throws Exception {
        SearchResponse<ProductSearchDocument> emptyResponse = emptySearchResponse();

        when(elasticsearchClient.search(any(Function.class), eq(ProductSearchDocument.class)))
                .thenReturn(emptyResponse);

        CursorPageResponse<ProductSearchResponse> pageMin = service.search("coffee", -5, null);
        assertThat(pageMin.size()).isEqualTo(1);

        CursorPageResponse<ProductSearchResponse> pageMax = service.search("coffee", 10_000, null);
        assertThat(pageMax.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("search: maps hits to API response and produces a nextCursor from the last hit sort")
    void searchMapsHitsAndReturnsNextCursor() throws Exception {
        List<FieldValue> lastSort = List.of(FieldValue.of(0.99), FieldValue.of("id-2"));

        ProductSearchDocument doc1 = new ProductSearchDocument(
                "id-1",
                "Coffee",
                "Nice",
                1234L,
                "EUR",
                "EUR",
                "1234",
                Map.of("origin", "Ethiopia"),
                "origin Ethiopia"
        );
        ProductSearchDocument doc2 = new ProductSearchDocument(
                "id-2",
                "Coffee 2",
                "Nice 2",
                2000L,
                "EUR",
                "EUR",
                "2000",
                Map.of(),
                ""
        );

        Hit<ProductSearchDocument> hit1 = Hit.of(h -> h
                .index("products")
                .id("id-1")
                .source(doc1)
                .sort(List.of(FieldValue.of(1.0), FieldValue.of("id-1")))
        );

        Hit<ProductSearchDocument> hit2 = Hit.of(h -> h
                .index("products")
                .id("id-2")
                .source(doc2)
                .sort(lastSort)
        );

        SearchResponse<ProductSearchDocument> response = searchResponseWithHits(List.of(hit1, hit2));

        when(elasticsearchClient.search(any(Function.class), eq(ProductSearchDocument.class)))
                .thenReturn(response);
        when(cursorCodec.encodeCursorFromSort(lastSort)).thenReturn("cursor-2");

        CursorPageResponse<ProductSearchResponse> page = service.search("coffee", 25, null);

        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).name()).isEqualTo("Coffee");
        assertThat(page.items().get(0).price()).isEqualTo(new BigDecimal("12.34"));
        assertThat(page.nextCursor()).isEqualTo("cursor-2");

        verify(elasticsearchClient).search(searchFnCaptor.capture(), eq(ProductSearchDocument.class));

        SearchRequest req = searchFnCaptor.getValue().apply(new SearchRequest.Builder()).build();
        assertThat(req.index()).contains("products");
        assertThat(req.size()).isEqualTo(25);
        assertThat(req.sort()).hasSize(2);
    }

    @Test
    @DisplayName("search: when cursor is provided, decodes it into searchAfter")
    void searchWhenCursorProvidedDecodesSearchAfter() throws Exception {
        when(cursorCodec.decodeCursorToSearchAfter("c1"))
                .thenReturn(List.of(FieldValue.of(0.1), FieldValue.of("id")));

        SearchResponse<ProductSearchDocument> emptyResponse = emptySearchResponse();

        when(elasticsearchClient.search(any(Function.class), eq(ProductSearchDocument.class)))
                .thenReturn(emptyResponse);

        CursorPageResponse<ProductSearchResponse> page = service.search("coffee", 10, "c1");

        assertThat(page.items()).isEmpty();

        verify(elasticsearchClient).search(searchFnCaptor.capture(), eq(ProductSearchDocument.class));
        SearchRequest req = searchFnCaptor.getValue().apply(new SearchRequest.Builder()).build();
        assertThat(req.searchAfter()).isNotEmpty();

        verify(cursorCodec).decodeCursorToSearchAfter("c1");
    }

    @Test
    @DisplayName("search: wraps IOExceptions in ElasticSearchFailedSearchException")
    void searchWhenIOExceptionThrowsDomainException() throws Exception {
        when(elasticsearchClient.search(any(Function.class), eq(ProductSearchDocument.class)))
                .thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> service.search("coffee", 10, null))
                .isInstanceOf(ElasticSearchFailedSearchException.class);
    }

    private SearchResponse<ProductSearchDocument> emptySearchResponse() {
        return searchResponseWithHits(List.of());
    }

    private SearchResponse<ProductSearchDocument> searchResponseWithHits(List<Hit<ProductSearchDocument>> hits) {
        // Provide required fields that the ES Java client enforces.
        ShardStatistics shards = ShardStatistics.of(s -> s
                .total(1)
                .successful(1)
                .failed(0)
        );

        TotalHits total = TotalHits.of(t -> t
                .value((long) hits.size())
                .relation(TotalHitsRelation.Eq)
        );

        HitsMetadata<ProductSearchDocument> hitsMetadata = HitsMetadata.<ProductSearchDocument>of(h -> h
                .total(total)
                .hits(hits)
        );

        return SearchResponse.<ProductSearchDocument>of(r -> r
                .took(1)
                .timedOut(false)
                .shards(shards)
                .hits(hitsMetadata)
        );
    }
}
