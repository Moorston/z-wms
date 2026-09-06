package com.xwms.core.move.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 移库单 */
@Data
@TableName("wms_move_order")
public class MoveOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String moveNo;

    /** 移库类型: NORMAL/BATCH/AUTO/REPLENISH/ADJUST */
    private String moveType;

    private String warehouseCode;
    private String ownerCode;
    private String fromArea;
    private String toArea;

    /** 状态: DRAFT/RELEASED/EXECUTING/PARTIAL/COMPLETED/CANCELLED */
    private String status;

    private Integer totalSku;
    private BigDecimal totalQty;
    private BigDecimal movedQty;
    private Integer priority;
    private String reason;
    private String remark;
    private String createdBy;
    private String executedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private LocalDateTime startedTime;
    private LocalDateTime completedTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /** 明细列表（请求体传入，非持久化） */
    @TableField(exist = false)
    private List<MoveDetail> details;
}
