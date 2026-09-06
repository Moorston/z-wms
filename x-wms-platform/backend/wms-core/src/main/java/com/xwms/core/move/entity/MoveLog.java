package com.xwms.core.move.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 移库流水 */
@Data
@TableName("wms_move_log")
public class MoveLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String moveNo;
    private String taskNo;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String batchNo;
    private String fromLocation;
    private String toLocation;
    private String fromContainer;
    private String toContainer;
    private BigDecimal moveQty;
    private String operator;
    private LocalDateTime actionTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
