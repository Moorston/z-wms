package com.xwms.core.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 快照对比 */
@Data
@TableName("wms_snapshot_compare")
public class SnapshotCompare {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对比号 */
    private String compareNo;

    /** 快照1 */
    private String snapshotNo1;

    /** 快照2 */
    private String snapshotNo2;

    private String warehouseCode;

    /** 对比类型: QUANTITY/COST/STATUS */
    private String compareType;

    /** 差异SKU数 */
    private Integer totalDiffSku;

    /** 差异总数量 */
    private BigDecimal totalDiffQty;

    /** 差异总成本 */
    private BigDecimal totalDiffCost;

    /** 状态 */
    private String status;

    private String operator;
    private LocalDateTime compareTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
