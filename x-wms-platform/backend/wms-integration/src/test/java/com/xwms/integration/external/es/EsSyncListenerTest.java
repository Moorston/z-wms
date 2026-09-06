package com.xwms.integration.external.es;

import java.util.List;
import java.util.Map;

import com.xwms.integration.external.es.event.EsSyncEvent;
import com.xwms.integration.external.es.listener.EsSyncListener;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ES 同步监听器单元测试
 *
 * <p>验证：
 * <ul>
 *   <li>正常同步：调用 bulkIndex</li>
 *   <li>ES 异常：不抛出异常，最终一致性</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class EsSyncListenerTest {

    @Mock private EsSyncClient esSyncClient;

    private EsSyncListener esSyncListener;

    @Test
    void onEsSyncEvent_callsBulkIndex() throws Exception {
        esSyncListener = new EsSyncListener(esSyncClient);

        List<Object> documents = List.of(
                Map.of("logNo", "APILOG001"),
                Map.of("logNo", "APILOG002")
        );
        EsSyncEvent event = new EsSyncEvent(this, documents, "wms-api-call-log");

        doNothing().when(esSyncClient).bulkIndex("wms-api-call-log", documents);

        esSyncListener.onEsSyncEvent(event);

        verify(esSyncClient).bulkIndex("wms-api-call-log", documents);
    }

    @Test
    void onEsSyncEvent_esException_doesNotThrow() throws Exception {
        esSyncListener = new EsSyncListener(esSyncClient);

        List<Object> documents = List.of(
                Map.of("messageId", "MSG-001")
        );
        EsSyncEvent event = new EsSyncEvent(this, documents, "wms-integration-message");

        doThrow(new RuntimeException("ES connection refused"))
                .when(esSyncClient).bulkIndex("wms-integration-message", documents);

        // 不应抛出异常
        assertDoesNotThrow(() -> esSyncListener.onEsSyncEvent(event));
    }

    @Test
    void onEsSyncEvent_emptyList_callsBulkIndexWithEmptyList() throws Exception {
        esSyncListener = new EsSyncListener(esSyncClient);

        List<Object> documents = List.of();
        EsSyncEvent event = new EsSyncEvent(this, documents, "wms-callback-record");

        doNothing().when(esSyncClient).bulkIndex("wms-callback-record", documents);

        esSyncListener.onEsSyncEvent(event);

        verify(esSyncClient).bulkIndex("wms-callback-record", documents);
    }
}
