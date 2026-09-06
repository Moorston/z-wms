package com.xwms.analytics.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 结算记录 */
@Data
@TableName("wms_billing_settlement")
public class BillingSettlement {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String settlementNo;
    private Long billId;
    private String billNo;
    private String ownerCode;

    /** 结算类型: FULL全额/PARTIAL部分/ADVANCE预付 */
    private String settlementType;

    private BigDecimal amount;
    private String currency;

    /** 支付方式: BANK_TRANSFER/CREDIT_CREDIT/CASH/OTHER */
    private String paymentMethod;

    /** 支付凭证号 */
    private String paymentRef;

    private LocalDate settlementDate;

    /** 状态: PENDING/COMPLETED/FAILED */
    private String status;

    private String operator;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
