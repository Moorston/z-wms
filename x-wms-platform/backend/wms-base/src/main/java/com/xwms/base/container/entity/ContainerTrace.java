package com.xwms.base.container.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 容器流转记录 */
@Data
@TableName("wms_container_trace")
public class ContainerTrace {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String traceNo;
    private String containerNo;

    /** 操作类型: BIND/RELEASE/MOVE/CLEAN/REPAIR/DISCARD */
    private String actionType;

    private String fromLocation;
    private String toLocation;
    private String refType;
    private String refNo;
    private BigDecimal quantity;
    private String operator;
    private LocalDateTime actionTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
