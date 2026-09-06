package com.xwms.core.safety.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 安全库存结果 */
@Data
@TableName("wms_safety_stock")
public class SafetyStock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String stockId;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String categoryCode;
    private String abcClass;

    /** 安全库存 */
    private BigDecimal safetyStock;

    /** 补货点 */
    private BigDecimal reorderPoint;

    /** 最高库存 */
    private BigDecimal maxStock;

    /** 平均日需求 */
    private BigDecimal avgDemand;

    /** 需求标准差 */
    private BigDecimal stdDemand;

    /** 提前期 */
    private BigDecimal leadTime;

    /** 服务水平 */
    private BigDecimal serviceLevel;

    /** 计算方法 */
    private String calcMethod;

    /** 当前库存 */
    private BigDecimal currentStock;

    /** 库存状态: NORMAL/BELOW_SAFETY/BELOW_REORDER/OUT_OF_STOCK/OVER_STOCK */
    private String stockStatus;

    private LocalDateTime calcTime;

    /** 状态: CURRENT/HISTORY */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
