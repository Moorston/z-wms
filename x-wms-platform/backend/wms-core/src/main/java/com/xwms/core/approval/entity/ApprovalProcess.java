package com.xwms.core.approval.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 审批流程定义 */
@Data
@TableName("wms_approval_process")
public class ApprovalProcess {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String processCode;
    private String processName;

    /**
     * 流程类型:
     * INVENTORY_ADJUST/INVENTORY_MOVE/INVENTORY_FREEZE/INVENTORY_UNFREEZE/STOCKTAKE_DIFF/RETURN/PURCHASE/OTHER
     */
    private String processType;

    private String warehouseCode;
    private String ownerCode;

    /** 版本号 */
    private Integer version;

    private String description;

    /** 状态: ACTIVE/INACTIVE/DRAFT */
    private String status;

    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
