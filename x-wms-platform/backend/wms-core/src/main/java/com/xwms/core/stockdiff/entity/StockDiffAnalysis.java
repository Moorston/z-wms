package com.xwms.core.stockdiff.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 差异分析汇总 */
@Data
@TableName("wms_stock_diff_analysis")
public class StockDiffAnalysis {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String analysisId;
    private LocalDate analysisDate;

    /** 分析类型: DAILY/MONTHLY/STOCKTAKE */
    private String analysisType;

    private String warehouseCode;
    private String ownerCode;
    private String categoryCode;
    private String skuCode;
    private String locationCode;

    private Integer totalCount;
    private Integer diffCount;
    private Integer shortageCount;
    private Integer overageCount;

    private BigDecimal totalSystemQty;
    private BigDecimal totalCountedQty;
    private BigDecimal totalDiffQty;

    /** 准确率(%) */
    private BigDecimal accuracyRate;

    /** 差异率(%) */
    private BigDecimal diffRate;

    private Integer majorDiffCount;
    private Integer criticalDiffCount;

    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
