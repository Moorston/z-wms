package com.xwms.core.multiwms.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 仓库间调拨明细 */
@Data
@TableName("wms_transfer_detail")
public class TransferDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long transferId;
    private String transferNo;
    private String sku;
    private String productName;
    private String batchNo;
    private String fromLocation;
    private String toLocation;

    private BigDecimal plannedQty;
    private BigDecimal shippedQty;
    private BigDecimal receivedQty;
    private BigDecimal differenceQty;

    private String unit;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
