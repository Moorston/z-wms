package com.xwms.core.expiry.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 过期处理记录 */
@Data
@TableName("wms_expiry_handle_log")
public class ExpiryHandleLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 处理单号 */
    private String handleNo;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String batchNo;

    private LocalDate expiryDate;

    /** 处理类型: FREEZE/RETURN/DESTROY/SELL/ADJUST */
    private String handleType;

    /** 处理数量 */
    private BigDecimal handleQty;

    /** 处理原因 */
    private String handleReason;

    /** 关联单号 */
    private String refNo;

    private String operator;
    private String approver;
    private LocalDateTime approveTime;
    private LocalDateTime handleTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
