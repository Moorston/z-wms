package com.xwms.core.pool.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存共享规则 */
@Data
@TableName("wms_share_rule")
public class ShareRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String warehouseCode;
    private String poolCode;

    /** 共享类型: OWNER/SKU/CATEGORY/ALL */
    private String shareType;

    private String sourceOwner;
    private String targetOwner;
    private String sourceSku;
    private String targetSku;
    private String categoryCode;

    /** 共享比例(%) */
    private BigDecimal shareRatio;

    private Integer priority;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
