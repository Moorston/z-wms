package com.xwms.core.inbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 入库单 */
@Data
@TableName("wms_inbound_order")
public class InboundOrder {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String inboundNo;

    /** 入库类型: PURCHASE/RETURN/TRANSFER/PRODUCTION/BLIND */
    private String inboundType;

    private String asnNo;
    private String refNo;
    private String supplierCode;

    /** 货主编码(业务字段) */
    private String ownerCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String warehouseCode;

    /** 收货库区 */
    private String areaCode;

    private BigDecimal totalQty;
    private BigDecimal receivedQty;
    private BigDecimal putawayQty;

    /** 状态: CREATED/RECEIVING/RECEIVED/QCING/PUTAWAYING/DONE/CANCELLED */
    private String status;

    private LocalDateTime receiveTime;
    private LocalDateTime putawayTime;
    private LocalDateTime doneTime;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
