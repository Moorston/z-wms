package com.xwms.core.shipment.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 发运作业记录 */
@Data
@TableName("wms_shipment_task")
public class ShipmentTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private String shipmentNo;

    /** 作业类型: GET_TRACKING/PRINT/PICKUP/DELIVER */
    private String taskType;

    private String expressNo;
    private String carrier;

    /** 状态: PENDING/PROCESSING/SUCCESS/FAILED */
    private String status;

    private Integer retryCount;
    private String errorMsg;
    private String operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
