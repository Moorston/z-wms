package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 退货明细 */
@Data
@TableName("wms_return_item")
public class ReturnItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long returnId;
    private String returnNo;
    private Long sourceItemId;

    private String sku;
    private String barcode;
    private String productName;
    private String batchNo;
    private String ownerCode;

    private BigDecimal planQty;
    private BigDecimal receivedQty;
    private BigDecimal qualifiedQty;
    private BigDecimal unqualifiedQty;
    private BigDecimal putawayQty;

    private BigDecimal unitPrice;

    /** 明细状态: PENDING/RECEIVED/QC/PASSED/FAILED/PUTAWAYED/REJECTED */
    private String itemStatus;

    private String qcResult;
    private String qcRemark;
    private String putawayLocation;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
