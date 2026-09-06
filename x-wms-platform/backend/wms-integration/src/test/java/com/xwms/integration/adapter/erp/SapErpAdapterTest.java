package com.xwms.integration.adapter.erp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xwms.integration.adapter.erp.client.SapODataClient;
import com.xwms.integration.adapter.erp.client.SapODataClient.SapODataException;
import com.xwms.integration.adapter.erp.config.SapErpConfig;
import com.xwms.integration.adapter.erp.converter.SapErpMessageConverter;
import com.xwms.integration.adapter.erp.dto.SapErpDto;
import com.xwms.integration.api.idempotent.ApiIdempotentService;
import com.xwms.integration.core.retry.RetryExecutor;
import com.xwms.integration.external.service.IntegrationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SAP ERP 适配器单元测试
 *
 * <p>覆盖场景：
 * <ol>
 *   <li>I1 入库过账成功路径</li>
 *   <li>I1 入库过账幂等缓存命中</li>
 *   <li>I1 入库过账幂等处理中（并发冲突）</li>
 *   <li>I1 入库过账 SAP 业务错误（4xx）</li>
 *   <li>I2 出库过账成功路径</li>
 *   <li>I3 库存同步成功路径</li>
 *   <li>I4 物料主数据拉取成功</li>
 *   <li>I5 仓库主数据拉取成功</li>
 *   <li>I1 字段校验失败（缺失必填）</li>
 * </ol>
 */
class SapErpAdapterTest {

    @Mock private SapODataClient oDataClient;
    @Mock private SapErpMessageConverter converter;
    @Mock private ApiIdempotentService idempotentService;
    @Mock private RetryExecutor retryExecutor;
    @Mock private IntegrationService integrationService;

    @InjectMocks private SapErpAdapter adapter;

    private SapErpConfig config;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        config = new SapErpConfig();
        config.setBaseUrl("https://sap.test/opu/odata");
        config.setUsername("user");
        config.setPassword("pass");
        config.setPlant("P001");
        config.setStorageLocation("WH01");
        config.setEnabled(true);
        // 注入配置到 adapter（因为 @InjectMocks 无法注入未 mock 字段）
        try {
            java.lang.reflect.Field f = SapErpAdapter.class.getDeclaredField("sapErpConfig");
            f.setAccessible(true);
            f.set(adapter, config);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ============================================================
    // I1 入库过账
    // ============================================================

    @Test
    void testPostInbound_success() {
        // 1. 幂等获取成功
        when(idempotentService.tryAcquire(anyString())).thenReturn(true);
        // 2. 字段转换成功
        SapErpDto.GoodsMovementPayload payload = new SapErpDto.GoodsMovementPayload();
        when(converter.toInboundPayload(any())).thenReturn(payload);
        // 3. 重试执行器直接执行 supplier
        Map<String, Object> sapResponse = Map.of(
                "d", Map.of("MVMT_HEADER", Map.of("DocNum", "5000012345")));
        when(converter.extractDocNum(sapResponse)).thenReturn("5000012345");
        when(retryExecutor.executeWithRetry(any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<Object> supplier = inv.getArgument(0);
            return supplier.get();
        });
        when(oDataClient.postWithCsrf(anyString(), any())).thenReturn(sapResponse);

        SapErpDto.InboundPostRequest request = createInboundRequest();
        SapErpDto.GoodsMovementResponse response = adapter.postInboundGoods(request);

        assertTrue(response.isSuccess());
        assertEquals("5000012345", response.getSapDocNum());
        verify(idempotentService).markSuccess(eq("sap:postin:INB001"), eq("5000012345"));
    }

    @Test
    void testPostInbound_idempotentCached() {
        // 幂等键命中，直接返回缓存结果
        when(idempotentService.tryAcquire(anyString())).thenReturn(false);
        when(idempotentService.getCachedResult("sap:postin:INB001")).thenReturn("5000012345");

        SapErpDto.InboundPostRequest request = createInboundRequest();
        SapErpDto.GoodsMovementResponse response = adapter.postInboundGoods(request);

        assertTrue(response.isSuccess());
        assertEquals("5000012345", response.getSapDocNum());
        assertEquals(0L, response.getCostTimeMs());
        verify(oDataClient, never()).postWithCsrf(anyString(), any());
    }

    @Test
    void testPostInbound_idempotentProcessing() {
        // 幂等键命中但正在处理中
        when(idempotentService.tryAcquire(anyString())).thenReturn(false);
        when(idempotentService.getCachedResult(anyString())).thenReturn(null);
        when(idempotentService.isProcessing(anyString())).thenReturn(true);

        SapErpDto.InboundPostRequest request = createInboundRequest();
        SapErpDto.GoodsMovementResponse response = adapter.postInboundGoods(request);

        assertFalse(response.isSuccess());
        assertEquals("409", response.getErrorCode());
        verify(oDataClient, never()).postWithCsrf(anyString(), any());
    }

    @Test
    void testPostInbound_sapBusinessError() {
        when(idempotentService.tryAcquire(anyString())).thenReturn(true);
        when(converter.toInboundPayload(any())).thenReturn(new SapErpDto.GoodsMovementPayload());
        when(retryExecutor.executeWithRetry(any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<Object> supplier = inv.getArgument(0);
            return supplier.get();
        });
        // SAP 返回业务错误（4xx）
        when(oDataClient.postWithCsrf(anyString(), any()))
                .thenThrow(new SapODataException("SAP 业务错误", 400, "物料不存在"));

        SapErpDto.InboundPostRequest request = createInboundRequest();
        SapErpDto.GoodsMovementResponse response = adapter.postInboundGoods(request);

        assertFalse(response.isSuccess());
        assertEquals("400", response.getErrorCode());
        verify(idempotentService).markFailed("sap:postin:INB001");
    }

    @Test
    void testPostInbound_invalidRequest_missingField() {
        when(idempotentService.tryAcquire(anyString())).thenReturn(true);
        // 转换时抛 IllegalArgumentException
        when(converter.toInboundPayload(any()))
                .thenThrow(new IllegalArgumentException("字段缺失或为空: inboundNo"));

        SapErpDto.InboundPostRequest request = createInboundRequest();
        request.setInboundNo(null); // 缺失必填
        SapErpDto.GoodsMovementResponse response = adapter.postInboundGoods(request);

        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains("inboundNo"));
    }

    // ============================================================
    // I2 出库过账
    // ============================================================

    @Test
    void testPostOutbound_success() {
        when(idempotentService.tryAcquire(anyString())).thenReturn(true);
        when(converter.toOutboundPayload(any())).thenReturn(new SapErpDto.GoodsMovementPayload());
        Map<String, Object> sapResponse = Map.of("d", Map.of("MVMT_HEADER", Map.of("DocNum", "6000000001")));
        when(converter.extractDocNum(sapResponse)).thenReturn("6000000001");
        when(retryExecutor.executeWithRetry(any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<Object> supplier = inv.getArgument(0);
            return supplier.get();
        });
        when(oDataClient.postWithCsrf(anyString(), any())).thenReturn(sapResponse);

        SapErpDto.OutboundPostRequest request = new SapErpDto.OutboundPostRequest();
        request.setShipmentNo("SHP001");
        request.setItems(List.of(createItem()));

        SapErpDto.GoodsMovementResponse response = adapter.postOutboundGoods(request);

        assertTrue(response.isSuccess());
        assertEquals("6000000001", response.getSapDocNum());
        verify(idempotentService).markSuccess(eq("sap:postout:SHP001"), eq("6000000001"));
    }

    // ============================================================
    // I3 库存同步
    // ============================================================

    @Test
    void testSyncInventory_success() {
        when(idempotentService.tryAcquire(anyString())).thenReturn(true);
        when(converter.toInventoryPayload(any())).thenReturn(new SapErpDto.InventoryPayload());
        when(retryExecutor.executeWithRetry(any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<Object> supplier = inv.getArgument(0);
            return supplier.get();
        });
        when(oDataClient.postWithCsrf(anyString(), any())).thenReturn(Map.of());

        SapErpDto.InventorySyncRequest request = new SapErpDto.InventorySyncRequest();
        request.setMaterial("SKU-001");
        request.setSkuCode("SKU-001");
        request.setWarehouseCode("WH01");
        request.setStockQty(BigDecimal.valueOf(1000));
        request.setStockUom("EA");

        SapErpDto.InventorySyncResponse response = adapter.syncInventory(request);

        assertTrue(response.isSuccess());
        verify(idempotentService).markSuccess(eq("sap:syncc:WH01:SKU-001"), eq("SYNC_OK"));
    }

    @Test
    void testSyncInventory_duplicateRequest() {
        when(idempotentService.tryAcquire(anyString())).thenReturn(false);
        when(idempotentService.getCachedResult(anyString())).thenReturn("SYNC_OK");

        SapErpDto.InventorySyncRequest request = new SapErpDto.InventorySyncRequest();
        request.setMaterial("SKU-001");
        request.setSkuCode("SKU-001");
        request.setWarehouseCode("WH01");
        request.setStockQty(BigDecimal.valueOf(1000));

        SapErpDto.InventorySyncResponse response = adapter.syncInventory(request);

        assertTrue(response.isSuccess());
        assertEquals(0L, response.getCostTimeMs());
        verify(oDataClient, never()).postWithCsrf(anyString(), any());
    }

    // ============================================================
    // I4 物料主数据拉取
    // ============================================================

    @Test
    void testPullMaterialMaster_success() {
        when(retryExecutor.executeWithRetry(any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<Object> supplier = inv.getArgument(0);
            return supplier.get();
        });
        Map<String, Object> sapResponse = Map.of("d", Map.of("results", List.of(
                Map.of("Material", "SKU-001", "MaterialDescription", "示例物料", "MaterialType", "ROH",
                        "BaseUnit", "EA", "IsActive", "true"))));
        when(oDataClient.getWithParams(anyString(), any())).thenReturn(sapResponse);
        when(converter.toMaterialMasterItems(sapResponse)).thenReturn(List.of(new SapErpDto.MaterialMasterItem()));

        SapErpDto.MaterialMasterQueryRequest request = new SapErpDto.MaterialMasterQueryRequest();
        request.setActiveOnly(true);

        SapErpDto.MasterDataPullResponse<SapErpDto.MaterialMasterItem> response =
                adapter.pullMaterialMaster(request);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void testPullMaterialMaster_failure() {
        when(retryExecutor.executeWithRetry(any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<Object> supplier = inv.getArgument(0);
            return supplier.get();
        });
        when(oDataClient.getWithParams(anyString(), any()))
                .thenThrow(new SapODataException("SAP 连接超时", null, null));

        SapErpDto.MaterialMasterQueryRequest request = new SapErpDto.MaterialMasterQueryRequest();
        SapErpDto.MasterDataPullResponse<SapErpDto.MaterialMasterItem> response =
                adapter.pullMaterialMaster(request);

        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorMessage());
    }

    // ============================================================
    // I5 仓库主数据拉取
    // ============================================================

    @Test
    void testPullWarehouseMaster_success() {
        when(retryExecutor.executeWithRetry(any(), any())).thenAnswer(inv -> {
            java.util.function.Supplier<Object> supplier = inv.getArgument(0);
            return supplier.get();
        });
        Map<String, Object> sapResponse = Map.of("d", Map.of("results", List.of(
                Map.of("Plant", "P001", "PlantName", "示例工厂",
                        "StorageLocation", "WH01", "StorageLocationName", "主仓库",
                        "IsActive", "true"))));
        when(oDataClient.getWithParams(anyString(), any())).thenReturn(sapResponse);
        when(converter.toWarehouseMasterItems(sapResponse))
                .thenReturn(List.of(new SapErpDto.WarehouseMasterItem()));

        SapErpDto.WarehouseMasterQueryRequest request = new SapErpDto.WarehouseMasterQueryRequest();
        request.setPlant("P001");

        SapErpDto.MasterDataPullResponse<SapErpDto.WarehouseMasterItem> response =
                adapter.pullWarehouseMaster(request);

        assertTrue(response.isSuccess());
        assertEquals(1, response.getItems().size());
    }

    // ============================================================
    // 健康检查
    // ============================================================

    @Test
    void testHealthCheck_success() {
        when(oDataClient.getWithParams(anyString(), any())).thenReturn(Map.of());
        assertTrue(adapter.healthCheck());
    }

    @Test
    void testHealthCheck_disabled() {
        config.setEnabled(false);
        assertFalse(adapter.healthCheck());
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private SapErpDto.InboundPostRequest createInboundRequest() {
        SapErpDto.InboundPostRequest request = new SapErpDto.InboundPostRequest();
        request.setInboundNo("INB001");
        request.setWarehouseCode("WH01");
        request.setPostingDate(LocalDate.now());
        request.setDocDate(LocalDate.now());
        request.setPoNumber("PO-2026-0831");
        request.setItems(List.of(createItem()));
        return request;
    }

    private SapErpDto.GoodsMovementItem createItem() {
        SapErpDto.GoodsMovementItem item = new SapErpDto.GoodsMovementItem();
        item.setMaterial("SKU-001");
        item.setBatch("B20260831-01");
        item.setMvmtQuantity(BigDecimal.valueOf(100));
        item.setEntryUom("EA");
        item.setTargetLocation("LOC-A-01-01");
        item.setPoItem("00010");
        return item;
    }
}
