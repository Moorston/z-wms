package com.xwms.core.inbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 入库明细 */
@Data
@TableName("wms_inbound_detail")
public class InboundDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String detailNo;
    private String inboundNo;
    private Integer lineNo;
    private String skuCode;
    private String skuName;

    /** 商品条码 */
    private String barcode;

    /** 批号 */
    private String batchNo;

    /** 收货库位 */
    private String receiveLocation;

    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    private BigDecimal putawayQty;
    private String unit;
    private String packageCode;

    /** 状态: CREATED/RECEIVED/PUTAWAYING/DONE */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
