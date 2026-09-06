package com.xwms.analytics.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 账单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_billing_bill")
public class BillingBill extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String billNo;

    /** 账单类型: MONTHLY月结/DAILY日结/ADHOC临时 */
    private String billType;

    private String ownerCode;
    private String customerCode;
    private String warehouseCode;

    /** 账单期间: 202608 */
    private String billPeriod;

    private LocalDate startDate;
    private LocalDate endDate;

    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    private String currency;

    /** 状态: DRAFT/PENDING/CONFIRMED/INVOICED/PARTIAL_PAID/PAID/OVERDUE/CANCELLED */
    private String status;

    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate paidDate;

    private String invoiceNo;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
