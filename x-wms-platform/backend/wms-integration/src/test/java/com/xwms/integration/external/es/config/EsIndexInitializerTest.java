package com.xwms.integration.external.es.config;

import java.util.List;
import java.util.function.Function;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import co.elastic.clients.util.ObjectBuilder;
import co.elastic.clients.transport.endpoints.BooleanResponse;

import com.xwms.integration.external.es.EsSyncClient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ES 索引初始化器单元测试
 */
@ExtendWith(MockitoExtension.class)
class EsIndexInitializerTest {

    @Mock private EsSyncClient esSyncClient;
    @Mock private ElasticsearchClient esClient;
    @Mock private ElasticsearchIndicesClient indicesClient;

    @Test
    void constructor_createsAllThreeIndexes() throws Exception {
        when(esSyncClient.getClient()).thenReturn(esClient);
        when(esClient.indices()).thenReturn(indicesClient);

        // exists 使用 lambda：e -> e.index(indexName)，返回 BooleanResponse
        BooleanResponse existsFalse = mock(BooleanResponse.class);
        when(existsFalse.value()).thenReturn(false);
        when(indicesClient.exists(any(Function.class))).thenReturn(existsFalse);

        // 创建成功（合并为一次 stubbing，避免 UnfinishedStubbing）
        ArgumentCaptor<CreateIndexRequest> captor =
                ArgumentCaptor.forClass(CreateIndexRequest.class);
        when(indicesClient.create(captor.capture()))
                .thenReturn(CreateIndexResponse.of(r -> r.acknowledged(true)
                        .shardsAcknowledged(true)
                        .index("wms-api-call-log")));

        // 触发构造函数
        new EsIndexInitializer(esSyncClient);

        // 验证 3 个索引都被创建
        verify(indicesClient, times(3)).exists(any(Function.class));

        List<CreateIndexRequest> requests = captor.getAllValues();
        assertEquals(3, requests.size());
        assertEquals("wms-api-call-log", requests.get(0).index());
        assertEquals("wms-integration-message", requests.get(1).index());
        assertEquals("wms-callback-record", requests.get(2).index());
    }

    @Test
    void constructor_skipsExistingIndexes() throws Exception {
        when(esSyncClient.getClient()).thenReturn(esClient);
        when(esClient.indices()).thenReturn(indicesClient);

        BooleanResponse existsTrue = mock(BooleanResponse.class);
        when(existsTrue.value()).thenReturn(true);
        when(indicesClient.exists(any(Function.class))).thenReturn(existsTrue);

        new EsIndexInitializer(esSyncClient);

        verify(indicesClient, times(3)).exists(any(Function.class));
        verify(indicesClient, never()).create(any(CreateIndexRequest.class));
    }

    @Test
    void constructor_esUnavailable_doesNotThrow() throws Exception {
        when(esSyncClient.getClient()).thenReturn(esClient);
        when(esClient.indices()).thenReturn(indicesClient);

        when(indicesClient.exists(any(Function.class)))
                .thenThrow(new java.io.IOException("ES unavailable"));

        assertDoesNotThrow(() -> new EsIndexInitializer(esSyncClient));
    }
}
