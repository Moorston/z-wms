package com.xwms.core.expiry.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 效期规则 */
@Data
@TableName("wms_expiry_rule")
public class ExpiryRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String warehouseCode;
    private String ownerCode;
    private String categoryCode;
    private String skuCode;

    /** 保质期(天) */
    private Integer shelfLifeDays;

    /** 一级预警(天) */
    private Integer warningDays1;

    /** 二级预警(天) */
    private Integer warningDays2;

    /** 三级预警(天) */
    private Integer warningDays3;

    /** 入库最小剩余天数(天) */
    private Integer inboundExpiryDays;

    /** 入库预警天数(天) */
    private Integer inboundWarningDays;

    /** 出库最小剩余天数(天) */
    private Integer outboundExpiryDays;

    /** 出库预警天数(天) */
    private Integer outboundWarningDays;

    /** 过期处理: FREEZE/RETURN/DESTROY/SELL */
    private String expiryAction;

    /** 是否启用FEFO: Y/N */
    private String fefoEnable;

    /** 过期自动冻结: Y/N */
    private String autoFreeze;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private Integer priority;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
