package com.xwms.core.abc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** XYZ分类结果（需求波动性分类） */
@Data
@TableName("wms_xyz_classification")
public class XyzClassification {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String classifyId;
    private String warehouseCode;
    private String skuCode;

    /** XYZ分类: X/Y/Z */
    private String xyzClass;

    /** 平均需求 */
    private BigDecimal avgDemand;

    /** 标准差 */
    private BigDecimal stdDev;

    /** 变异系数(CV=标准差/均值) */
    private BigDecimal cv;

    private BigDecimal maxDemand;
    private BigDecimal minDemand;

    /** X类CV阈值 */
    private BigDecimal demandCvThresholdX;

    /** Y类CV阈值 */
    private BigDecimal demandCvThresholdY;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;
    private LocalDateTime classifyTime;

    /** 状态: CURRENT/HISTORY */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
