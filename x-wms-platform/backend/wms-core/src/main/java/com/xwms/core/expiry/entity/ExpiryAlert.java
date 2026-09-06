package com.xwms.core.expiry.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 临期预警 */
@Data
@TableName("wms_expiry_alert")
public class ExpiryAlert {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 预警单号 */
    private String alertNo;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String batchNo;

    private LocalDate expiryDate;
    private Integer remainDays;

    /** 预警级别: W1/W2/W3 */
    private String warningLevel;

    /** 预警数量 */
    private BigDecimal alertQty;

    /** 预警类型: NEAR_EXPIRY/EXPIRED */
    private String alertType;

    /** 状态: PENDING/PROCESSING/RESOLVED/IGNORED */
    private String status;

    /** 处理措施 */
    private String handleAction;

    private String handledBy;
    private LocalDateTime handledTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
