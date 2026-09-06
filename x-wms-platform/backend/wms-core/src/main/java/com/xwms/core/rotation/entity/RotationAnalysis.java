package com.xwms.core.rotation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 周转分析 */
@Data
@TableName("wms_rotation_analysis")
public class RotationAnalysis {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 统计日期 */
    private LocalDateTime analysisDate;

    /** 周期类型: DAY/WEEK/MONTH */
    private String periodType;

    private String skuCode;
    private String categoryCode;
    private String warehouseCode;
    private String ownerCode;

    /** 期初数量 */
    private BigDecimal openingQty;

    /** 入库数量 */
    private BigDecimal inboundQty;

    /** 出库数量 */
    private BigDecimal outboundQty;

    /** 期末数量 */
    private BigDecimal closingQty;

    /** 周转率 */
    private BigDecimal turnoverRate;

    /** 周转天数 */
    private BigDecimal turnoverDays;

    /** 平均库存 */
    private BigDecimal avgInventory;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
