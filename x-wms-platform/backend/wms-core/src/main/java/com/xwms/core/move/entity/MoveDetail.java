package com.xwms.core.move.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 移库明细 */
@Data
@TableName("wms_move_detail")
public class MoveDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String moveNo;
    private Integer lineNo;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private String fromLocation;
    private String toLocation;
    private String fromContainer;
    private String toContainer;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 已移数量 */
    private BigDecimal movedQty;

    /** 状态: PENDING/MOVING/COMPLETED/CANCELLED */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
