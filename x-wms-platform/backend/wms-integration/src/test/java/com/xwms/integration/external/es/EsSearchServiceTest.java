package com.xwms.integration.external.es;

import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import com.xwms.integration.external.es.document.ApiCallLogDocument;
import com.xwms.integration.external.es.document.IntegrationMessageDocument;
import com.xwms.integration.external.es.document.CallbackRecordDocument;
import com.xwms.integration.external.es.dto.EsSearchRequest;
import com.xwms.integration.external.es.dto.EsSearchResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ES 检索服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class EsSearchServiceTest {

    @Mock private EsSyncClient esSyncClient;
    @Mock private ElasticsearchClient esClient;

    private EsSearchService esSearchService;

    @BeforeEach
    void setUp() throws Exception {
        when(esSyncClient.getClient()).thenReturn(esClient);
        esSearchService = new EsSearchService(esSyncClient);
    }

    // ============================================================
    // searchApiCallLogs
    // ============================================================

    @Test
    void searchApiCallLogs_returnsResults() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setKeyword("Order001");
        req.setPage(0);
        req.setSize(10);

        // Mock ES response
        ApiCallLogDocument doc = new ApiCallLogDocument();
        doc.setLogNo("APILOG001");
        doc.setSystemCode("ERP");
        doc.setBusinessNo("Order001");

        Hit<ApiCallLogDocument> hit = Hit.of(h -> h
                .id("APILOG001")
                .index("wms-api-call-log")
                .source(doc));

        SearchResponse<ApiCallLogDocument> mockResp = SearchResponse.of(s -> s
                .took(100L)
                .timedOut(false)
                .hits(h -> h
                        .total(TotalHits.of(t -> t.value(1).relation(TotalHitsRelation.Eq)))
                        .hits(java.util.List.of(hit))));

        when(esClient.search(any(SearchRequest.class), eq(ApiCallLogDocument.class)))
                .thenReturn(mockResp);

        EsSearchResponse<ApiCallLogDocument> result = esSearchService.searchApiCallLogs(req);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("APILOG001", result.getItems().get(0).getLogNo());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
    }

    @Test
    void searchApiCallLogs_esException_returnsEmptyResult() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setKeyword("test");

        when(esClient.search(any(SearchRequest.class), eq(ApiCallLogDocument.class)))
                .thenThrow(new RuntimeException("ES unavailable"));

        EsSearchResponse<ApiCallLogDocument> result = esSearchService.searchApiCallLogs(req);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
    }

    @Test
    void searchApiCallLogs_buildsCorrectQueryWithKeyword() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setKeyword("Order001");
        req.setSystemCode("ERP");
        req.setStatus("SUCCESS");

        Hit<ApiCallLogDocument> hit = Hit.of(h -> h.id("APILOG001").index("wms-api-call-log").source(null));
        SearchResponse<ApiCallLogDocument> mockResp = SearchResponse.of(s -> s
                .took(100L)
                .timedOut(false)
                .hits(h -> h
                        .total(TotalHits.of(t -> t.value(1).relation(TotalHitsRelation.Eq)))
                        .hits(java.util.List.of(hit))));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        when(esClient.search(captor.capture(), eq(ApiCallLogDocument.class)))
                .thenReturn(mockResp);

        esSearchService.searchApiCallLogs(req);

        SearchRequest searchReq = captor.getValue();
        assertNotNull(searchReq.query());
        Query query = searchReq.query();

        // 验证查询包含 bool
        assertNotNull(query.bool());
        assertNotNull(query.bool().must());
        assertFalse(query.bool().must().isEmpty());

        // 验证 filter 不为空
        assertNotNull(query.bool().filter());
        assertFalse(query.bool().filter().isEmpty());

        // 验证 from 和 size
        assertEquals(0, searchReq.from());
        assertEquals(20, searchReq.size());
    }

    // ============================================================
    // searchMessages
    // ============================================================

    @Test
    void searchMessages_returnsResults() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setSystemCode("TMS");
        req.setPage(1);
        req.setSize(5);

        IntegrationMessageDocument doc = new IntegrationMessageDocument();
        doc.setMessageId("MSG-001");
        doc.setSystemCode("TMS");
        doc.setStatus("SENT");

        Hit<IntegrationMessageDocument> hit = Hit.of(h -> h
                .id("MSG-001")
                .index("wms-integration-message")
                .source(doc));

        SearchResponse<IntegrationMessageDocument> mockResp = SearchResponse.of(s -> s
                .took(100L)
                .timedOut(false)
                .hits(h -> h
                        .total(TotalHits.of(t -> t.value(3).relation(TotalHitsRelation.Eq)))
                        .hits(java.util.List.of(hit))));

        when(esClient.search(any(SearchRequest.class), eq(IntegrationMessageDocument.class)))
                .thenReturn(mockResp);

        EsSearchResponse<IntegrationMessageDocument> result = esSearchService.searchMessages(req);

        assertNotNull(result);
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("MSG-001", result.getItems().get(0).getMessageId());
        assertEquals(1, result.getPage());
        assertEquals(5, result.getSize());
    }

    // ============================================================
    // searchCallbacks
    // ============================================================

    @Test
    void searchCallbacks_returnsResults() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setTraceId("TRACE-123");
        req.setPage(0);
        req.setSize(20);

        CallbackRecordDocument doc = new CallbackRecordDocument();
        doc.setCallbackNo("CB-001");
        doc.setSystemCode("WCS");
        doc.setStatus("SUCCESS");

        Hit<CallbackRecordDocument> hit = Hit.of(h -> h
                .id("CB-001")
                .index("wms-callback-record")
                .source(doc));

        SearchResponse<CallbackRecordDocument> mockResp = SearchResponse.of(s -> s
                .took(100L)
                .timedOut(false)
                .hits(h -> h
                        .total(TotalHits.of(t -> t.value(2).relation(TotalHitsRelation.Eq)))
                        .hits(java.util.List.of(hit))));

        when(esClient.search(any(SearchRequest.class), eq(CallbackRecordDocument.class)))
                .thenReturn(mockResp);

        EsSearchResponse<CallbackRecordDocument> result = esSearchService.searchCallbacks(req);

        assertNotNull(result);
        assertEquals(2, result.getTotal());
        assertEquals(1, result.getItems().size());
        assertEquals("CB-001", result.getItems().get(0).getCallbackNo());
    }

    // ============================================================
    // searchAll (跨索引聚合)
    // ============================================================

    @Test
    void searchAll_aggregatesResultsFromAllIndexes() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setKeyword("Order001");

        // Mock 三个索引的响应（生产代码用 raw type Map.class，doReturn 绕过泛型检查）
        SearchResponse<Map<String, Object>> mockResp = SearchResponse.of(s -> s
                .took(100L)
                .timedOut(false)
                .hits(h -> h
                        .total(TotalHits.of(t -> t.value(1).relation(TotalHitsRelation.Eq)))
                        .hits(java.util.List.of(
                                Hit.of(hit -> hit
                                        .id("1")
                                        .index("wms-api-call-log")
                                        .source(Map.of("field1", "value1")))
                        ))));

        doReturn(mockResp).when(esClient)
                .search(any(SearchRequest.class), eq(Map.class));

        EsSearchResponse<Map<String, Object>> result = esSearchService.searchAll(req);

        assertNotNull(result);
        assertEquals(3, result.getTotal());
        assertEquals(3, result.getItems().size());
    }

    @Test
    void searchAll_esException_returnsEmptyResult() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setKeyword("test");

        when(esClient.search(any(SearchRequest.class), eq(Map.class)))
                .thenThrow(new RuntimeException("ES unavailable"));

        EsSearchResponse<Map<String, Object>> result = esSearchService.searchAll(req);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotal());
    }

    // ============================================================
    // 分页与排序
    // ============================================================

    @Test
    void searchApiCallLogs_appliesPagination() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setPage(2);
        req.setSize(15);

        Hit<ApiCallLogDocument> hit = Hit.of(h -> h.id("1").index("wms-api-call-log").source(null));
        SearchResponse<ApiCallLogDocument> mockResp = SearchResponse.of(s -> s
                .took(100L)
                .timedOut(false)
                .hits(h -> h
                        .total(TotalHits.of(t -> t.value(50).relation(TotalHitsRelation.Eq)))
                        .hits(java.util.List.of(hit))));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        when(esClient.search(captor.capture(), eq(ApiCallLogDocument.class)))
                .thenReturn(mockResp);

        esSearchService.searchApiCallLogs(req);

        SearchRequest searchReq = captor.getValue();
        assertEquals(30, searchReq.from()); // page 2 * size 15
        assertEquals(15, searchReq.size());
    }

    @Test
    void searchApiCallLogs_appliesSorting() throws Exception {
        EsSearchRequest req = new EsSearchRequest();
        req.setSortField("createdTime");
        req.setSortOrder("asc");

        Hit<ApiCallLogDocument> hit = Hit.of(h -> h.id("1").index("wms-api-call-log").source(null));
        SearchResponse<ApiCallLogDocument> mockResp = SearchResponse.of(s -> s
                .took(100L)
                .timedOut(false)
                .hits(h -> h
                        .total(TotalHits.of(t -> t.value(1).relation(TotalHitsRelation.Eq)))
                        .hits(java.util.List.of(hit))));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        when(esClient.search(captor.capture(), eq(ApiCallLogDocument.class)))
                .thenReturn(mockResp);

        esSearchService.searchApiCallLogs(req);

        SearchRequest searchReq = captor.getValue();
        assertFalse(searchReq.sort().isEmpty());
    }
}
