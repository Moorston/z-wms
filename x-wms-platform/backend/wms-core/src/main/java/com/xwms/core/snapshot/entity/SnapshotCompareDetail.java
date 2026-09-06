package com.xwms.core.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 快照对比明细 */
@Data
@TableName("wms_snapshot_compare_detail")
public class SnapshotCompareDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 对比号 */
    private String compareNo;

    private String skuCode;
    private String locationCode;
    private String batchNo;

    /** 快照1数量 */
    private BigDecimal qty1;

    /** 快照2数量 */
    private BigDecimal qty2;

    /** 数量差异 */
    private BigDecimal diffQty;

    /** 快照1成本 */
    private BigDecimal cost1;

    /** 快照2成本 */
    private BigDecimal cost2;

    /** 成本差异 */
    private BigDecimal diffCost;

    /** 差异类型: INCREASE/DECREASE/NEW/REMOVED */
    private String diffType;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
