package com.xwms.core.transfer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 调拨在途库存 */
@Data
@TableName("wms_transfer_in_transit")
public class TransferInTransit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String transferNo;
    private Integer lineNo;
    private String skuCode;
    private String batchNo;
    private String fromWarehouse;
    private String toWarehouse;

    /** 在途数量 */
    private BigDecimal inTransitQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 状态: IN_TRANSIT/PARTIAL_RECEIVED/RECEIVED */
    private String status;

    private LocalDateTime shipTime;
    private LocalDateTime expectedArrival;
    private LocalDateTime receiveTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
