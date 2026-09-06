package com.xwms.integration.external.es;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ES 同步客户端单元测试
 */
class EsSyncClientTest {

    private EsSyncClient esSyncClient;

    @Test
    void extractId_fromApiCallLog_returnsLogNo() throws Exception {
        esSyncClient = new EsSyncClient("localhost", 9200, "http", "", "");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("logNo", "APILOG20260831120000001");
        doc.put("systemCode", "ERP");

        String id = esSyncClient.extractId(doc);

        assertEquals("APILOG20260831120000001", id);
    }

    @Test
    void extractId_fromIntegrationMessage_returnsMessageId() throws Exception {
        esSyncClient = new EsSyncClient("localhost", 9200, "http", "", "");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("messageId", "MSG-UUID-12345");
        doc.put("systemCode", "TMS");

        String id = esSyncClient.extractId(doc);

        assertEquals("MSG-UUID-12345", id);
    }

    @Test
    void extractId_fromCallbackRecord_returnsCallbackNo() throws Exception {
        esSyncClient = new EsSyncClient("localhost", 9200, "http", "", "");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("callbackNo", "CB20260831120000002");
        doc.put("systemCode", "WCS");

        String id = esSyncClient.extractId(doc);

        assertEquals("CB20260831120000002", id);
    }

    @Test
    void extractId_logNoTakesPriority_overMessageId() throws Exception {
        esSyncClient = new EsSyncClient("localhost", 9200, "http", "", "");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("logNo", "LOG-1");
        doc.put("messageId", "MSG-2");

        String id = esSyncClient.extractId(doc);

        assertEquals("LOG-1", id);
    }

    @Test
    void extractId_messageIdTakesPriority_overCallbackNo() throws Exception {
        esSyncClient = new EsSyncClient("localhost", 9200, "http", "", "");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("messageId", "MSG-2");
        doc.put("callbackNo", "CB-3");

        String id = esSyncClient.extractId(doc);

        assertEquals("MSG-2", id);
    }

    @Test
    void extractId_noKnownField_returnsRandomUuid() throws Exception {
        esSyncClient = new EsSyncClient("localhost", 9200, "http", "", "");

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("unknownField", "value");

        String id = esSyncClient.extractId(doc);

        assertNotNull(id);
        assertFalse(id.isEmpty());
        assertEquals(32, id.length()); // UUID without dashes
    }

    @Test
    void extractId_nullDoc_returnsRandomUuid() throws Exception {
        esSyncClient = new EsSyncClient("localhost", 9200, "http", "", "");

        String id = esSyncClient.extractId(null);

        assertNotNull(id);
        assertFalse(id.isEmpty());
        assertEquals(32, id.length());
    }
}
