package com.xwms.integration.external.es;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xwms.integration.core.model.ApiResponse;
import com.xwms.integration.external.es.document.ApiCallLogDocument;
import com.xwms.integration.external.es.document.CallbackRecordDocument;
import com.xwms.integration.external.es.document.IntegrationMessageDocument;
import com.xwms.integration.external.es.dto.EsSearchRequest;
import com.xwms.integration.external.es.dto.EsSearchResponse;

import lombok.RequiredArgsConstructor;

/**
 * ES 全文检索 REST 接口
 *
 * <p>搜索入口：
 *
 * <ul>
 *   <li>{@code GET /es/api-logs} — 搜索 API 调用日志
 *   <li>{@code GET /es/messages} — 搜索集成消息流水
 *   <li>{@code GET /es/callbacks} — 搜索回调记录
 *   <li>{@code GET /es/all} — 跨索引聚合搜索
 * </ul>
 */
@RestController
@RequestMapping("/es")
@RequiredArgsConstructor
public class EsSearchController {

    private final EsSearchService esSearchService;

    @GetMapping("/api-logs")
    public ApiResponse searchApiCallLogs(EsSearchRequest req) {
        EsSearchResponse<ApiCallLogDocument> resp = esSearchService.searchApiCallLogs(req);
        return ApiResponse.success(resp);
    }

    @GetMapping("/messages")
    public ApiResponse searchMessages(EsSearchRequest req) {
        EsSearchResponse<IntegrationMessageDocument> resp = esSearchService.searchMessages(req);
        return ApiResponse.success(resp);
    }

    @GetMapping("/callbacks")
    public ApiResponse searchCallbacks(EsSearchRequest req) {
        EsSearchResponse<CallbackRecordDocument> resp = esSearchService.searchCallbacks(req);
        return ApiResponse.success(resp);
    }

    @GetMapping("/all")
    public ApiResponse searchAll(EsSearchRequest req) {
        EsSearchResponse<Map<String, Object>> resp = esSearchService.searchAll(req);
        return ApiResponse.success(resp);
    }
}
