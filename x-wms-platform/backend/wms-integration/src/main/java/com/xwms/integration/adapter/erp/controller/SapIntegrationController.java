package com.xwms.integration.adapter.erp.controller;

import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import com.xwms.common.core.Result;
import com.xwms.integration.adapter.erp.SapErpAdapter;
import com.xwms.integration.adapter.erp.dto.SapErpDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * SAP ERP 集成 REST 入口
 *
 * <p>暴露 5 类集成接口供 WMS 主系统业务域调用：
 *
 * <ul>
 *   <li>{@code POST /api/sap/inbound/post} — I1 入库过账
 *   <li>{@code POST /api/sap/outbound/post} — I2 出库过账
 *   <li>{@code POST /api/sap/inventory/sync} — I3 库存同步
 *   <li>{@code POST /api/sap/material/pull} — I4 物料主数据拉取
 *   <li>{@code POST /api/sap/warehouse/pull} — I5 仓库主数据拉取
 * </ul>
 *
 * <p>所有接口自动生成 traceId 并写入 MDC，便于全链路追踪。
 */
@Tag(name = "SAP ERP集成", description = "SAP S/4HANA 过账/库存/主数据同步")
@RestController
@RequestMapping("/api/sap")
@RequiredArgsConstructor
public class SapIntegrationController {

    private final SapErpAdapter sapErpAdapter;

    @Operation(summary = "I1 入库过账")
    @PostMapping("/inbound/post")
    public Result<SapErpDto.GoodsMovementResponse> postInbound(
            @RequestBody SapErpDto.InboundPostRequest request) {
        ensureTraceId();
        return Result.success(sapErpAdapter.postInboundGoods(request));
    }

    @Operation(summary = "I2 出库过账")
    @PostMapping("/outbound/post")
    public Result<SapErpDto.GoodsMovementResponse> postOutbound(
            @RequestBody SapErpDto.OutboundPostRequest request) {
        ensureTraceId();
        return Result.success(sapErpAdapter.postOutboundGoods(request));
    }

    @Operation(summary = "I3 库存同步")
    @PostMapping("/inventory/sync")
    public Result<SapErpDto.InventorySyncResponse> syncInventory(
            @RequestBody SapErpDto.InventorySyncRequest request) {
        ensureTraceId();
        return Result.success(sapErpAdapter.syncInventory(request));
    }

    @Operation(summary = "I4 物料主数据拉取（SAP → WMS）")
    @PostMapping("/material/pull")
    public Result<SapErpDto.MasterDataPullResponse<SapErpDto.MaterialMasterItem>> pullMaterial(
            @RequestBody(required = false) SapErpDto.MaterialMasterQueryRequest request) {
        ensureTraceId();
        if (request == null) request = new SapErpDto.MaterialMasterQueryRequest();
        return Result.success(sapErpAdapter.pullMaterialMaster(request));
    }

    @Operation(summary = "I5 仓库/库位主数据拉取（SAP → WMS）")
    @PostMapping("/warehouse/pull")
    public Result<SapErpDto.MasterDataPullResponse<SapErpDto.WarehouseMasterItem>> pullWarehouse(
            @RequestBody(required = false) SapErpDto.WarehouseMasterQueryRequest request) {
        ensureTraceId();
        if (request == null) request = new SapErpDto.WarehouseMasterQueryRequest();
        return Result.success(sapErpAdapter.pullWarehouseMaster(request));
    }

    @Operation(summary = "SAP 健康检查")
    @GetMapping("/health")
    public Result<Boolean> healthCheck() {
        ensureTraceId();
        return Result.success(sapErpAdapter.healthCheck());
    }

    private void ensureTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isEmpty()) {
            MDC.put("traceId", UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        }
    }
}
