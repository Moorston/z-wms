package com.xwms.base.serial.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 序列号流转记录 */
@Data
@TableName("wms_serial_trace")
public class SerialTrace {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String traceNo;
    private String serialNo;

    /** 操作类型: GENERATE/INBOUND/PUTAWAY/PICK/PACK/SHIP/RETURN/SCRAP/MOVE */
    private String actionType;

    private String fromStatus;
    private String toStatus;
    private String fromLocation;
    private String toLocation;
    private String refType;
    private String refNo;
    private String operator;
    private LocalDateTime actionTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
