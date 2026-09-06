package com.xwms.core.shipment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 发运明细 */
@Data
@TableName("wms_shipment_detail")
public class ShipmentDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String shipmentNo;
    private Integer lineNo;
    private String packageNo;
    private String skuCode;
    private String batchNo;
    private BigDecimal shippedQty;
    private BigDecimal weight;
    private BigDecimal volume;

    /** 状态: PENDING/SHIPPED/DELIVERED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
