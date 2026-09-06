package com.xwms.integration.adapter.erp.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * SAP ERP 集成 DTO 契约层
 *
 * <p>本类汇总 5 类接口（入库过账/出库过账/库存同步/物料主数据/仓库主数据）的请求与响应 DTO。 所有 DTO 均为不可变字段，便于序列化与测试断言。
 *
 * <p>命名规则：
 *
 * <ul>
 *   <li>{@code *Request} — WMS 侧原始业务入参
 *   <li>{@code *Response} — WMS 侧统一响应（含 sapDocNum 便于回写业务表）
 *   <li>{@code SapODataPayload} — 发送给 SAP 的 OData V2 载荷
 * </ul>
 */
public final class SapErpDto {

    private SapErpDto() {}

    // ============================================================
    // I1 入库过账
    // ============================================================

    /** 入库过账请求 */
    @Data
    public static class InboundPostRequest {
        /** WMS 入库单号（幂等键的一部分） */
        private String inboundNo;

        /** 仓库代码 */
        private String warehouseCode;

        /** SAP 工厂代码 */
        private String plant;

        /** SAP 库存地点 */
        private String storageLocation;

        /** 过账日期 */
        private LocalDate postingDate;

        /** 凭证日期 */
        private LocalDate docDate;

        /** 参考单号（默认=入库单号） */
        private String refDocument;

        /** 报告关键字 */
        private String rptKeyField;

        /** 采购订单号（采购入库场景） */
        private String poNumber;

        /** 明细行 */
        private List<GoodsMovementItem> items;
    }

    // ============================================================
    // I2 出库过账
    // ============================================================

    /** 出库过账请求 */
    @Data
    public static class OutboundPostRequest {
        /** WMS 出库单号（幂等键的一部分） */
        private String shipmentNo;

        /** 仓库代码 */
        private String warehouseCode;

        /** SAP 工厂代码 */
        private String plant;

        /** SAP 库存地点 */
        private String storageLocation;

        /** 过账日期 */
        private LocalDate postingDate;

        /** 凭证日期 */
        private LocalDate docDate;

        /** 参考单号（默认=出库单号） */
        private String refDocument;

        /** 报告关键字 */
        private String rptKeyField;

        /** 销售订单号（销售出库场景） */
        private String salesOrder;

        /** 明细行 */
        private List<GoodsMovementItem> items;
    }

    /** 货物移动明细行（入库/出库共用） */
    @Data
    public static class GoodsMovementItem {
        /** 物料编号 */
        private String material;

        /** 批次号 */
        private String batch;

        /** 移动数量 */
        private BigDecimal mvmtQuantity;

        /** 单位 */
        private String entryUom;

        /** 目标库位（SAP 存储位置） */
        private String targetLocation;

        /** 采购订单号（采购场景，明细行级别） */
        private String poNumber;

        /** 采购订单行（采购场景） */
        private String poItem;

        /** 销售订单行（销售场景） */
        private String salesOrderItem;

        /** 特殊库存标识 Q/K/E */
        private String specialStockType;

        /** 特殊库存号 */
        private String specialStockNo;
    }

    // ============================================================
    // I3 库存同步
    // ============================================================

    /** 库存同步请求 */
    @Data
    public static class InventorySyncRequest {
        /** 物料编号 */
        private String material;

        /** 批次号 */
        private String batch;

        /** 工厂 */
        private String plant;

        /** 库存地点 */
        private String storageLocation;

        /** 仓库代码（WMS 侧，用于幂等键） */
        private String warehouseCode;

        /** 物料代码（WMS 侧，用于幂等键） */
        private String skuCode;

        /** 总库存 */
        private BigDecimal stockQty;

        /** 单位 */
        private String stockUom;

        /** 非限制使用库存 */
        private BigDecimal unrestrictedUseStockQty;

        /** 质检库存 */
        private BigDecimal qualityInspectionStockQty;

        /** 冻结库存 */
        private BigDecimal blockedStockQty;

        /** 评估库存 */
        private BigDecimal valuatedStockQty;
    }

    // ============================================================
    // I4 物料主数据同步（SAP → WMS）
    // ============================================================

    /** 物料主数据拉取请求参数 */
    @Data
    public static class MaterialMasterQueryRequest {
        /** 物料编号（为空时拉取全部） */
        private String material;

        /** 是否仅拉取启用状态物料 */
        private boolean activeOnly = true;

        /** 分页大小 */
        private int pageSize = 100;

        /** 分页跳过 */
        private int skip = 0;
    }

    /** 物料主数据实体（WMS 侧接收） */
    @Data
    public static class MaterialMasterItem {
        private String skuCode;
        private String skuName;
        private String materialType;
        private String categoryCode;
        private String unitCode;
        private BigDecimal netWeight;
        private BigDecimal grossWeight;
        private String weightUom;
        private BigDecimal length;
        private BigDecimal width;
        private BigDecimal height;
        private String dimensionUom;
        private String externalCode;
        private LocalDate createdTime;
        private Boolean active;
    }

    // ============================================================
    // I5 仓库/库位主数据同步（SAP → WMS）
    // ============================================================

    /** 仓库主数据拉取请求参数 */
    @Data
    public static class WarehouseMasterQueryRequest {
        /** 工厂代码（为空时拉取全部） */
        private String plant;

        /** 是否仅拉取启用状态 */
        private boolean activeOnly = true;

        private int pageSize = 100;
        private int skip = 0;
    }

    /** 仓库主数据实体（WMS 侧接收） */
    @Data
    public static class WarehouseMasterItem {
        private String warehouseCode;
        private String warehouseName;
        private String storageAreaCode;
        private String storageAreaName;
        private String storageType;
        private Boolean active;
    }

    // ============================================================
    // 统一响应
    // ============================================================

    /** SAP 过账统一响应 */
    @Data
    public static class GoodsMovementResponse {
        /** 是否成功 */
        private boolean success;

        /** SAP 凭证号（成功后返回，便于业务回写） */
        private String sapDocNum;

        /** SAP 错误消息 */
        private String errorMessage;

        /** SAP 错误代码 */
        private String errorCode;

        /** 调用耗时（毫秒） */
        private long costTimeMs;
    }

    /** 库存同步响应 */
    @Data
    public static class InventorySyncResponse {
        private boolean success;
        private String errorMessage;
        private String errorCode;
        private long costTimeMs;
    }

    /** 主数据拉取响应 */
    @Data
    public static class MasterDataPullResponse<T> {
        private boolean success;
        private List<T> items;
        private int total;
        private String errorMessage;
    }

    // ============================================================
    // SAP OData 载荷（发送方向）
    // ============================================================

    /** 货物移动 OData 载荷（I1 入库 / I2 出库 共用） */
    @Data
    public static class GoodsMovementPayload {
        private Map<String, Object> goodsMvtHeader;
        private List<Map<String, Object>> goodsMvtItem;
    }

    /** 库存同步 OData 载荷 */
    @Data
    public static class InventoryPayload {
        private Map<String, Object> stockInTransit;
    }
}
