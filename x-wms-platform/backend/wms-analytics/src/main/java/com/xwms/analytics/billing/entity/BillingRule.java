package com.xwms.analytics.billing.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 计费规则 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_billing_rule")
public class BillingRule extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 费用类型: STORAGE/INBOUND/OUTBOUND/HANDLING/VAS/OTHER */
    private String feeType;

    /** 计费方式: PER_UNIT/PER_ORDER/PER_WEIGHT/PER_VOLUME/PER_DAY/PER_MONTH/FLAT/STEP */
    private String chargeMode;

    private BigDecimal unitPrice;
    private String currency;
    private BigDecimal minCharge;
    private BigDecimal maxCharge;
    private BigDecimal freeQty;

    /** 阶梯计费配置(JSON) */
    private String stepConfig;

    private LocalDate effectiveDate;
    private LocalDate expireDate;

    /** 适用货主(空=全部) */
    private String ownerCode;

    /** 适用客户(空=全部) */
    private String customerCode;

    private String warehouseCode;
    private Integer priority;

    /** 状态: ENABLED/DISABLED */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
