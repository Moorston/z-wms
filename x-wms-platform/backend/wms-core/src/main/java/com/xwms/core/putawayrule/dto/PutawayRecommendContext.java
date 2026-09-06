package com.xwms.core.putawayrule.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Data;

/** 上架推荐上下文 包含上架请求的所有信息，用于规则匹配、库位校验和排序打分 */
@Data
public class PutawayRecommendContext {

    /** 仓库编码 */
    private String warehouseCode;

    /** 货主编码 */
    private String ownerCode;

    /** SKU编码 */
    private String skuCode;

    /** 产品组 */
    private String productGroup;

    /** 循环级别（A/B/C） */
    private String cycleLevel;

    /** 批次号 */
    private String lot;

    /** 生产日期 */
    private java.time.LocalDate productionDate;

    /** 有效期 */
    private java.time.LocalDate expiryDate;

    /** 批次属性Map（key=属性名, value=属性值） */
    private Map<String, String> batchAttrs;

    /** 上架数量（主单位） */
    private BigDecimal quantity;

    /** 包装级别：PALLET整托/CASE整箱/EACH零散 */
    private String packageLevel;

    /** 订单类型：NORMAL正常/RETURN退货/SAMPLE样品/TRANSFER调拨 */
    private String orderType;

    /** LPN（托盘号） */
    private String lpn;

    /** 收货单号 */
    private String receiptNo;

    /** 产品体积(m³) */
    private BigDecimal productVolume;

    /** 产品重量(kg) */
    private BigDecimal productWeight;

    /** 产品长(mm) */
    private BigDecimal productLength;

    /** 产品宽(mm) */
    private BigDecimal productWidth;

    /** 产品高(mm) */
    private BigDecimal productHeight;

    /** 托盘数 */
    private Integer palletCount;

    /** 箱数 */
    private Integer caseCount;

    /** 收货库位（用于收货库位分流策略） */
    private String receiveLocation;

    /** 操作人 */
    private String operator;
}
