package com.xwms.integration.adapter.erp.dto;

/**
 * SAP 字段映射常量与移动类型枚举
 *
 * <p>集中管理 WMS ↔ SAP 字段名映射、移动类型码、存储类型码等常量，避免散落。 新增或修改字段映射只需改本类，无需触及业务代码。
 */
public final class SapErpFieldMapping {

    private SapErpFieldMapping() {}

    // ============================================================
    // SAP 端点（路径）
    // ============================================================

    /** 货物移动过账端点 */
    public static final String ENDPOINT_GOODS_MVT = "/API_GOODSMVT_CREATE";

    /** 货物移动 Header 集合 */
    public static final String ENDPOINT_GOODS_MVT_HEADER =
            ENDPOINT_GOODS_MVT + "/GoodsMvtHeaderSet";

    /** 库存同步端点 */
    public static final String ENDPOINT_STOCK_IN_TRANSIT =
            "/API_STOCK_IN_TRANSIT_SRV/StockInTransit";

    /** 物料主数据端点 */
    public static final String ENDPOINT_MATERIAL_SRV = "/API_MATERIAL_SRV/A_Material";

    /** 仓库/库位主数据端点 */
    public static final String ENDPOINT_STORAGE_LOCATION_SRV =
            "/API_STORAGELOCATION_SRV/PlantStorageLocationSet";

    // ============================================================
    // OData 字段名（SAP 侧）
    // ============================================================

    // --- GoodsMovement Header ---
    public static final String F_MVMT_TYPE = "MvmtType";
    public static final String F_MVMT_REASON = "MvmtReason";
    public static final String F_POSTNG_DATE = "PstngDate";
    public static final String F_DOC_DATE = "DocDate";
    public static final String F_REF_DOC_NO = "RefDocNo";
    public static final String F_RPT_KEY_FLD = "RptKeyFld";
    public static final String F_PLANT = "Plant";
    public static final String F_STGE_LOC = "StgeLoc";

    // --- GoodsMovement Item ---
    public static final String F_MATERIAL = "Material";
    public static final String F_BATCH = "Batch";
    public static final String F_MVMT_QUANT = "MvmtQuant";
    public static final String F_ENTRY_UOM = "EntryUom";
    public static final String F_PURCH_DOC = "PurchDoc";
    public static final String F_PURCH_DOC_IT = "PurchDocIt";
    public static final String F_SALES_ORD = "SalesOrd";
    public static final String F_SALES_ORD_IT = "SalesOrdIt";
    public static final String F_SPL_STOCK = "SplStock";
    public static final String F_SPL_STOCK_NO = "SplStockNo";

    // --- StockInTransit ---
    public static final String F_LBKUM = "Lbkum";
    public static final String F_LEHMEN = "Lehmen";
    public static final String F_LABOR = "Labor";
    public static final String F_QLMNG = "Qlmng";
    public static final String F_SPMNG = "Spmng";
    public static final String F_CHARG = "Charg";
    public static final String F_WERKS = "Werks";
    public static final String F_LGORT = "Lgort";
    public static final String F_MATNR = "Matnr";

    // --- Material Master ---
    public static final String F_MM_MATERIAL = "Material";
    public static final String F_MM_DESC = "MaterialDescription";
    public static final String F_MM_TYPE = "MaterialType";
    public static final String F_MM_GROUP = "MaterialGroup";
    public static final String F_MM_UNIT = "BaseUnit";
    public static final String F_MM_NET_WEIGHT = "NetWeight";
    public static final String F_MM_GROSS_WEIGHT = "GrossWeight";
    public static final String F_MM_WEIGHT_UOM = "WeightUOM";
    public static final String F_MM_LENGTH = "Length";
    public static final String F_MM_WIDTH = "Width";
    public static final String F_MM_HEIGHT = "Height";
    public static final String F_MM_DIM_UOM = "DImUoMCode";
    public static final String F_MM_EXT_NO = "ExternalMaterialNo";
    public static final String F_MM_CREATED = "CreatedOn";
    public static final String F_MM_ACTIVE = "IsActive";

    // --- Storage Location (Warehouse) ---
    public static final String F_WH_PLANT = "Plant";
    public static final String F_WH_PLANT_NAME = "PlantName";
    public static final String F_WH_STGE_LOC = "StorageLocation";
    public static final String F_WH_STGE_LOC_NAME = "StorageLocationName";
    public static final String F_WH_STGE_TYPE = "StorageType";

    // ============================================================
    // 移动类型码（GOODS MOVEMENT TYPE）
    // ============================================================

    /** 01=采购入库 */
    public static final String MVMT_TYPE_INBOUND_PURCHASE = "01";

    /** 02=生产入库 */
    public static final String MVMT_TYPE_INBOUND_PRODUCTION = "02";

    /** 03=其他入库 */
    public static final String MVMT_TYPE_INBOUND_OTHER = "03";

    /** 05=销售出库 */
    public static final String MVMT_TYPE_OUTBOUND_SALES = "05";

    /** 06=其他出库 */
    public static final String MVMT_TYPE_OUTBOUND_OTHER = "06";

    /** 移动原因：采购入库 */
    public static final String MVMT_REASON_INBOUND = "ZINB";

    /** 移动原因：销售出库 */
    public static final String MVMT_REASON_OUTBOUND = "ZOUT";

    // ============================================================
    // 默认值
    // ============================================================

    /** 默认报告关键字（WMS 系统标识） */
    public static final String DEFAULT_RPT_KEY_FLD = "3PL-WMS";

    /** 默认单位 */
    public static final String DEFAULT_UOM = "EA";

    // ============================================================
    // 幂等键前缀
    // ============================================================

    public static final String IDEMPOTENT_PREFIX_INBOUND = "sap:postin:";
    public static final String IDEMPOTENT_PREFIX_OUTBOUND = "sap:postout:";
    public static final String IDEMPOTENT_PREFIX_INVENTORY = "sap:syncc:";
    public static final String IDEMPOTENT_PREFIX_MATERIAL = "sap:md:";
    public static final String IDEMPOTENT_PREFIX_WAREHOUSE = "sap:wh:";
}
