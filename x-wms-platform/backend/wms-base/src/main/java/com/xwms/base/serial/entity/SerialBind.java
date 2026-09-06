package com.xwms.base.serial.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 序列号绑定（序列号与单据/容器的绑定关系） */
@Data
@TableName("wms_serial_bind")
public class SerialBind {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String bindNo;
    private String serialNo;

    /** 关联类型: INBOUND/OUTBOUND/TRANSFER/MOVE/STORAGE/RETURN */
    private String refType;

    private String refNo;
    private Integer refLineNo;
    private String containerNo;

    /** 状态: BOUND/RELEASED */
    private String status;

    private LocalDateTime bindTime;
    private LocalDateTime releaseTime;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
