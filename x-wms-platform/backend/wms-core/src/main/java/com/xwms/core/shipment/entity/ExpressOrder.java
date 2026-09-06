package com.xwms.core.shipment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 快递单 */
@Data
@TableName("wms_express_order")
public class ExpressOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String expressNo;

    /** 运单号 */
    private String trackingNo;

    private String shipmentNo;
    private String outboundNo;
    private String carrier;
    private String serviceType;

    /** 状态: CREATED/PRINTED/PICKED_UP/IN_TRANSIT/DELIVERED/FAILED/CANCELLED */
    private String status;

    private String senderName;
    private String senderPhone;
    private String senderAddress;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal shippingFee;

    private LocalDateTime printTime;
    private LocalDateTime pickupTime;
    private LocalDateTime deliveryTime;

    /** 获取/打印失败原因 */
    private String errorMsg;

    /** 重试次数 */
    private Integer retryCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
