package com.xwms.core.stockdiff.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 差异审批记录 */
@Data
@TableName("wms_stock_diff_approve")
public class StockDiffApprove {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String approveId;
    private String diffId;
    private String approveNode;
    private String approveRole;
    private String approver;

    /** 审批结果: APPROVED通过/REJECTED驳回/RETURNED退回 */
    private String approveResult;

    private String approveNote;
    private LocalDateTime approveTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
