package com.xwms.core.label.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 标签任务 */
@Data
@TableName("wms_label_task")
public class LabelTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskId;
    private String taskName;
    private String templateCode;
    private String warehouseCode;
    private String ownerCode;

    /** 业务类型: INBOUND/OUTBOUND/TRANSFER/INVENTORY/ADJUST/MOVE */
    private String bizType;

    private String bizNo;

    private Integer totalCount;
    private Integer printedCount;
    private Integer failedCount;

    private String printer;

    /** 打印模式: SINGLE单张/BATCH批量/QUEUE队列 */
    private String printMode;

    /** 状态: PENDING/PRINTING/COMPLETED/FAILED/CANCELLED */
    private String status;

    private String errorMessage;
    private String operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
