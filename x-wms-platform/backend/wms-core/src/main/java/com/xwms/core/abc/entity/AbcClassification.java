package com.xwms.core.abc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** ABC分类结果 */
@Data
@TableName("wms_abc_classification")
public class AbcClassification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 分类批次ID */
    private String classifyId;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String categoryCode;

    /** ABC分类: A/B/C */
    private String abcClass;

    /** XYZ分类: X/Y/Z */
    private String xyzClass;

    private BigDecimal salesAmount;
    private BigDecimal salesQty;
    private BigDecimal profit;
    private BigDecimal turnoverRate;
    private BigDecimal turnoverDays;
    private BigDecimal avgInventory;

    /** 累计占比(%) */
    private BigDecimal cumulativeRatio;

    /** 排名 */
    private Integer rankNo;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime classifyTime;

    /** 状态: CURRENT/HISTORY */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
