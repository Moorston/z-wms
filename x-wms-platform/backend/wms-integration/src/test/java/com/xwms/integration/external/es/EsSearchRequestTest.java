package com.xwms.integration.external.es;

import com.xwms.integration.external.es.dto.EsSearchRequest;
import com.xwms.integration.external.es.dto.EsSearchResponse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ES 搜索 DTO 单元测试
 */
class EsSearchRequestTest {

    @Test
    void defaultValues() {
        EsSearchRequest req = new EsSearchRequest();

        assertEquals(0, req.getPage());
        assertEquals(20, req.getSize());
        assertEquals("createdTime", req.getSortField());
        assertEquals("desc", req.getSortOrder());
    }

    @Test
    void customValues() {
        EsSearchRequest req = new EsSearchRequest();
        req.setKeyword("Order001");
        req.setSystemCode("ERP");
        req.setStatus("SUCCESS");
        req.setBusinessType("OUTBOUND");
        req.setBusinessNo("SN20260831001");
        req.setDirection("OUT");
        req.setTraceId("TRACE-12345");
        req.setStartTime("2026-08-31T00:00:00");
        req.setEndTime("2026-08-31T23:59:59");
        req.setPage(1);
        req.setSize(50);
        req.setSortField("createdTime");
        req.setSortOrder("asc");

        assertEquals("Order001", req.getKeyword());
        assertEquals("ERP", req.getSystemCode());
        assertEquals("SUCCESS", req.getStatus());
        assertEquals("OUTBOUND", req.getBusinessType());
        assertEquals("SN20260831001", req.getBusinessNo());
        assertEquals("OUT", req.getDirection());
        assertEquals("TRACE-12345", req.getTraceId());
        assertEquals("2026-08-31T00:00:00", req.getStartTime());
        assertEquals("2026-08-31T23:59:59", req.getEndTime());
        assertEquals(1, req.getPage());
        assertEquals(50, req.getSize());
        assertEquals("asc", req.getSortOrder());
    }
}

class EsSearchResponseTest {

    @Test
    void constructorSetsFields() {
        EsSearchResponse<String> resp = new EsSearchResponse<>(List.of("a", "b"), 2, 0, 20);

        assertEquals(2, resp.getTotal());
        assertEquals(2, resp.getItems().size());
        assertEquals(0, resp.getPage());
        assertEquals(20, resp.getSize());
        assertEquals("a", resp.getItems().get(0));
        assertEquals("b", resp.getItems().get(1));
    }

    @Test
    void emptyResponse() {
        EsSearchResponse<Object> resp = new EsSearchResponse<>(List.of(), 0, 0, 20);

        assertEquals(0, resp.getTotal());
        assertTrue(resp.getItems().isEmpty());
    }
}
