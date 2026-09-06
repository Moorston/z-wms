package com.xwms.core.crossdock.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 越库作业记录 */
@Data
@TableName("wms_crossdock_task")
public class CrossdockTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private String crossdockNo;

    /** 作业类型: RECEIVE收货/SORT分拣/SHIP发运 */
    private String taskType;

    private String skuCode;
    private String batchNo;

    /** 来源库位/月台 */
    private String fromLocation;

    /** 目标库位/月台 */
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
