package com.xwms.core.vas.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** VAS工单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_vas_order")
public class VasOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    /** 工单类型: INBOUND/OUTBOUND/STOCK/STANDALONE */
    private String orderType;

    /** 源单号 */
    private String sourceOrderNo;

    private String sourceOrderType;

    private String customerCode;
    private String ownerCode;
    private String warehouseCode;

    private String serviceCode;
    private String serviceName;
    private String serviceType;

    private BigDecimal planQty;
    private BigDecimal actualQty;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    /** 状态: PENDING/ASSIGNED/PROCESSING/PAUSED/COMPLETED/CANCELLED/EXCEPTION */
    private String status;

    private Integer priority;
    private String assignee;
    private LocalDateTime assignTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    /** 实际耗时(分钟) */
    private BigDecimal actualDuration;

    private LocalDateTime planStartTime;
    private LocalDateTime planFinishTime;

    /** 作业区/操作台 */
    private String locationCode;

    private String batchNo;
    private String remark;
    private String exceptionReason;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
