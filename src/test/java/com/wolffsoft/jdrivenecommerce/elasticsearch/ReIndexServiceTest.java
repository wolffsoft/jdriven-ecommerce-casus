package com.wolffsoft.jdrivenecommerce.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.elasticsearch.indices.RefreshResponse;
import com.wolffsoft.jdrivenecommerce.repository.ProductRepository;
import com.wolffsoft.jdrivenecommerce.repository.entity.ProductEntity;
import com.wolffsoft.jdrivenecommerce.service.elasticsearch.ReIndexService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReIndexServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private ElasticsearchIndicesClient indicesClient;

    @InjectMocks
    private ReIndexService reIndexService;

    @Captor
    private ArgumentCaptor<PageRequest> pageRequestCaptor;

    @Captor
    private ArgumentCaptor<BulkRequest> bulkRequestCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reIndexService, "indexName", "products");
        when(elasticsearchClient.indices()).thenReturn(indicesClient);
    }

    @Test
    @DisplayName("reindexAll: clamps batchSize to a safe range")
    void reindexAllClampsBatchSizeToSafeRange() {
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(Page.empty());

        reIndexService.reindexAll(10);

        verify(productRepository).findAll(pageRequestCaptor.capture());
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(100);

        reset(productRepository);
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(Page.empty());

        reIndexService.reindexAll(10_000);

        verify(productRepository).findAll(pageRequestCaptor.capture());
        assertThat(pageRequestCaptor.getValue().getPageSize()).isEqualTo(2000);
    }

    @Test
    @DisplayName("reindexAll: bulk indexes all products and refreshes the index")
    void reindexAllBulkIndexesAllProductsAndRefreshes() throws Exception {
        ProductEntity p1 = new ProductEntity(
                "Coffee",
                "Nice",
                1234L,
                "EUR",
                JsonUtil.toJson(Map.of("origin", "Ethiopia"))
        );
        p1.setId(UUID.randomUUID());

        ProductEntity p2 = new ProductEntity(
                "Tea",
                null,
                250L,
                "EUR",
                JsonUtil.toJson(Map.of())
        );
        p2.setId(UUID.randomUUID());

        Page<ProductEntity> page0 = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 100), 2);
        Page<ProductEntity> empty = Page.empty(PageRequest.of(1, 100));

        when(productRepository.findAll(any(PageRequest.class)))
                .thenReturn(page0)
                .thenReturn(empty);

        BulkResponse bulkOk = mock(BulkResponse.class);
        when(bulkOk.errors()).thenReturn(false);
        when(elasticsearchClient.bulk(any(BulkRequest.class))).thenReturn(bulkOk);

        // IMPORTANT: stub the correct overload (Function builder), not refresh(null)
        RefreshResponse refreshResponse = mock(RefreshResponse.class);
        when(indicesClient.refresh(any(Function.class))).thenReturn(refreshResponse);

        var result = reIndexService.reindexAll(100);

        assertThat(result.indexedCount()).isEqualTo(2);

        verify(elasticsearchClient).bulk(bulkRequestCaptor.capture());
        BulkRequest bulk = bulkRequestCaptor.getValue();
        assertThat(bulk.operations()).hasSize(2);

        Object firstDocObj = bulk.operations().getFirst().index().document();
        assertThat(firstDocObj).isInstanceOf(ProductSearchDocument.class);

        ProductSearchDocument firstDoc = (ProductSearchDocument) firstDocObj;
        assertThat(firstDoc.attributes()).containsEntry("origin", "Ethiopia");
        assertThat(firstDoc.attributesText()).contains("origin").contains("Ethiopia");

        verify(indicesClient).refresh(any(Function.class));
    }
}
