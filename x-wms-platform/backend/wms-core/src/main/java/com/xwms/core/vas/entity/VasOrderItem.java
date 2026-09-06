package com.xwms.core.vas.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** VAS工单明细 */
@Data
@TableName("wms_vas_order_item")
public class VasOrderItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;
    private String orderNo;

    private String sku;
    private String barcode;
    private String productName;
    private String batchNo;
    private String ownerCode;

    /** 源库位 */
    private String fromLocation;

    /** 目标库位 */
    private String toLocation;

    private BigDecimal planQty;
    private BigDecimal actualQty;
    private BigDecimal unitPrice;
    private BigDecimal itemAmount;

    /** 明细状态: PENDING/PROCESSING/COMPLETED/EXCEPTION */
    private String itemStatus;

    /** 加工前规格 */
    private String beforeSpec;

    /** 加工后规格 */
    private String afterSpec;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
