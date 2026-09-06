package com.xwms.analytics.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 账单明细 */
@Data
@TableName("wms_billing_bill_item")
public class BillingBillItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long billId;
    private String billNo;
    private Long feeItemId;

    private String feeType;
    private String feeName;

    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String currency;

    private LocalDate feeDate;
    private String refNo;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
