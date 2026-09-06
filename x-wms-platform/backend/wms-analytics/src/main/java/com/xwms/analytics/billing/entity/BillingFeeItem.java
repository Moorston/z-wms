package com.xwms.analytics.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 费用项目(流水) */
@Data
@TableName("wms_billing_fee_item")
public class BillingFeeItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String feeNo;

    /** 费用类型 */
    private String feeType;

    private Long ruleId;
    private String ruleCode;

    private String ownerCode;
    private String customerCode;
    private String warehouseCode;

    /** 关联业务类型: INBOUND/OUTBOUND/STORAGE/VAS */
    private String refType;

    /** 关联业务单号 */
    private String refNo;

    private Long refItemId;

    private String sku;
    private String productName;
    private String batchNo;

    /** 计费数量 */
    private BigDecimal quantity;

    /** 计费重量 */
    private BigDecimal weight;

    /** 计费体积 */
    private BigDecimal volume;

    /** 计费天数(仓储费) */
    private Integer days;

    private String chargeMode;
    private BigDecimal unitPrice;

    /** 费用金额 */
    private BigDecimal amount;

    private String currency;

    /** 费用发生日期 */
    private LocalDate feeDate;

    /** 费用期间: 202608 */
    private String feePeriod;

    /** 状态: PENDING待计费/BILLED已入账/SETTLED已结算/WRITTEN_OFF已核销 */
    private String status;

    /** 关联账单ID */
    private Long billId;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
