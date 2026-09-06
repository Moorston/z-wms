package com.xwms.core.snapshot.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 快照恢复记录 */
@Data
@TableName("wms_snapshot_restore_log")
public class SnapshotRestoreLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 恢复号 */
    private String restoreNo;

    /** 恢复的快照号 */
    private String snapshotNo;

    private String warehouseCode;

    /** 恢复类型: FULL/PARTIAL */
    private String restoreType;

    /** 恢复范围(SKU列表) */
    private String restoreScope;

    /** 恢复SKU数 */
    private Integer restoreSkuCount;

    /** 恢复总数量 */
    private BigDecimal restoreQty;

    /** 状态: PENDING/PROCESSING/COMPLETED/FAILED */
    private String status;

    private String operator;
    private LocalDateTime restoreTime;
    private String failReason;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
