package com.xwms.core.qc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 供应商质量评分 */
@Data
@TableName("wms_supplier_quality")
public class SupplierQuality {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 供应商编码 */
    private String supplierCode;

    /** 货主编码 */
    private String ownerCode;

    /** 统计周期: 2026-08 */
    private String statPeriod;

    /** 总批次 */
    private Integer totalBatches;

    /** 合格批次 */
    private Integer passedBatches;

    /** 总数量 */
    private BigDecimal totalQty;

    /** 不良数量 */
    private BigDecimal defectQty;

    /** 批次合格率(%) */
    private BigDecimal passRate;

    /** 不良率(%) */
    private BigDecimal defectRate;

    /** 交货及时率(%) */
    private BigDecimal onTimeRate;

    /** 综合评分 */
    private BigDecimal score;

    /** 等级: A/B/C/D */
    private String rankLevel;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
