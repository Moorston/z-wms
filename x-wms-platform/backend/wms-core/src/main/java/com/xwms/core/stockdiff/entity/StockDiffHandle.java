package com.xwms.core.stockdiff.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 差异处理记录 */
@Data
@TableName("wms_stock_diff_handle")
public class StockDiffHandle {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String handleId;
    private String diffId;

    /** 处理类型: ADJUST调整/RECOUNT复盘/TRANSFER转移/DAMAGE报损/OTHER其他 */
    private String handleType;

    private String handleAction;
    private String handleNote;
    private String beforeStatus;
    private String afterStatus;
    private String adjustNo;
    private BigDecimal adjustQty;
    private String operator;
    private LocalDateTime handleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
