package com.xwms.core.move.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 移库任务 */
@Data
@TableName("wms_move_task")
public class MoveTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private String moveNo;
    private Integer lineNo;

    /** 任务类型: MANUAL/WCS */
    private String taskType;

    private String assignee;
    private String equipmentCode;
    private String fromLocation;
    private String toLocation;
    private String skuCode;
    private String batchNo;
    private BigDecimal planQty;
    private BigDecimal movedQty;

    /** 状态: PENDING/ASSIGNED/EXECUTING/COMPLETED/CANCELLED */
    private String status;

    private LocalDateTime assignedTime;
    private LocalDateTime startedTime;
    private LocalDateTime completedTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
