package com.xwms.core.transfer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 调拨单 */
@Data
@TableName("wms_transfer")
public class Transfer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String transferNo;

    /** 调拨类型: NORMAL/URGENT/RETURN */
    private String transferType;

    /** 调出仓库 */
    private String fromWarehouse;

    /** 调入仓库 */
    private String toWarehouse;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    /** 状态: CREATED/APPROVED/SHIPPED/IN_TRANSIT/RECEIVED/DONE/CANCELLED */
    private String status;

    private BigDecimal totalQty;
    private BigDecimal shippedQty;
    private BigDecimal receivedQty;
    private BigDecimal differenceQty;

    private String carrier;
    private String trackingNo;
    private LocalDateTime expectedArrival;
    private LocalDateTime shipTime;
    private LocalDateTime receiveTime;
    private String remark;
    private String createdBy;
    private String approver;
    private LocalDateTime approveTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
