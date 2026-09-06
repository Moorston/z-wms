package com.xwms.core.outbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 出库单 */
@Data
@TableName("wms_outbound_order")
public class OutboundOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String outboundNo;

    /** 出库类型: SALE/TRANSFER/RETURN/VAS/SAMPLE */
    private String outboundType;

    /** 来源单号 */
    private String refNo;

    private String customerCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String warehouseCode;

    /** 波次号 */
    private String waveNo;

    private BigDecimal totalQty;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal packedQty;
    private BigDecimal shippedQty;

    /** 状态: CREATED/ALLOCATING/ALLOCATED/PICKING/PICKED/PACKING/PACKED/SHIPPING/SHIPPED/CANCELLED */
    private String status;

    private String carrier;
    private String trackingNo;
    private String deliveryMethod;
    private LocalDateTime expectedShipTime;
    private LocalDateTime actualShipTime;
    private String shippingAddress;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
