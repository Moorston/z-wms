package com.xwms.core.multiwms.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 订单多仓分配记录 */
@Data
@TableName("wms_order_allocation")
public class OrderAllocation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String allocationNo;
    private String orderNo;
    private String orderType;
    private String sku;
    private String productName;

    private BigDecimal requiredQty;
    private BigDecimal allocatedQty;

    /** 分配到哪个仓 */
    private String warehouseCode;

    /** 使用的分配规则 */
    private String allocationRule;

    /** 分配原因 */
    private String allocationReason;

    /** 状态: PENDING/ALLOCATED/SHIPPED/PARTIAL/FAILED */
    private String status;

    /** 优先级: LOW/NORMAL/HIGH/URGENT */
    private String priority;

    /** 收货地址(用于就近分配) */
    private String customerAddress;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
