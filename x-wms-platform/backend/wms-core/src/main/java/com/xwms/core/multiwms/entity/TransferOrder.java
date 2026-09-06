package com.xwms.core.multiwms.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 仓库间调拨单 */
@Data
@TableName("wms_transfer_order")
public class TransferOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String transferNo;

    /** 调拨类型: NORMAL/URGENT/BACKHAUL */
    private String transferType;

    private String fromWarehouse;
    private String toWarehouse;

    /** 状态: DRAFT/CONFIRMED/IN_TRANSIT/RECEIVED/COMPLETED/CANCELLED */
    private String status;

    /** 优先级: LOW/NORMAL/HIGH/URGENT */
    private String priority;

    private String carrier;
    private String trackingNo;

    private LocalDate plannedShipDate;
    private LocalDate actualShipDate;
    private LocalDate plannedArrivalDate;
    private LocalDate actualArrivalDate;

    private Integer totalSku;
    private BigDecimal totalQty;
    private BigDecimal shippedQty;
    private BigDecimal receivedQty;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private String confirmedBy;
    private LocalDateTime confirmedTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
