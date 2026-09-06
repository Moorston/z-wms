package com.xwms.core.transfer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 调拨作业记录 */
@Data
@TableName("wms_transfer_task")
public class TransferTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private String transferNo;

    /** 作业类型: SHIP发运/RECEIVE收货 */
    private String taskType;

    private String skuCode;
    private String batchNo;
    private String fromLocation;
    private String toLocation;
    private BigDecimal taskQty;
    private BigDecimal doneQty;
    private String operator;

    /** 状态: PENDING/PROCESSING/DONE/EXCEPTION */
    private String status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
