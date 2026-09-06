package com.xwms.core.shipment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 发运单 */
@Data
@TableName("wms_shipment")
public class Shipment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String shipmentNo;
    private String outboundNo;
    private String waveNo;
    private String warehouseCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String carrier;

    /** 服务类型: STANDARD/EXPRESS/NEXT_DAY/SAME_DAY/ECONOMY */
    private String serviceType;

    /** 状态: CREATED/PRINTED/PICKED_UP/IN_TRANSIT/DELIVERED/FAILED/CANCELLED */
    private String status;

    private BigDecimal totalQty;
    private BigDecimal shippedQty;
    private Integer packageCount;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;
    private BigDecimal shippingFee;

    private String senderName;
    private String senderPhone;
    private String senderAddress;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;

    private LocalDateTime expectedDelivery;
    private LocalDateTime shipTime;
    private LocalDateTime deliveryTime;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
