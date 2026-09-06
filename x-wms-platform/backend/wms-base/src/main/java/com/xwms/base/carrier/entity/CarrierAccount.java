package com.xwms.base.carrier.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 承运商账户 */
@Data
@TableName("wms_carrier_account")
public class CarrierAccount {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String carrierCode;

    /** 账户号/月结账号 */
    private String accountNo;

    /** 账户类型: MONTHLY/PREPAID/CASH */
    private String accountType;

    private BigDecimal balance;
    private BigDecimal creditLimit;
    private BigDecimal warningBalance;

    /** 状态: ACTIVE/FROZEN/CLOSED */
    private String status;

    private LocalDateTime lastRechargeTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
