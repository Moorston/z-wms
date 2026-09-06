package com.xwms.core.transfer.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 调拨明细 */
@Data
@TableName("wms_transfer_detail")
public class TransferDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String transferNo;
    private Integer lineNo;
    private String skuCode;
    private String batchNo;

    /** 调出库位 */
    private String fromLocation;

    /** 调入库位 */
    private String toLocation;

    private BigDecimal expectedQty;
    private BigDecimal shippedQty;
    private BigDecimal receivedQty;
    private BigDecimal differenceQty;

    /** 状态: PENDING/SHIPPED/IN_TRANSIT/RECEIVED/DONE */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
