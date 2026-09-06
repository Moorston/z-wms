package com.xwms.core.allocation.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 分配规则 */
@Data
@TableName("wms_allocation_rule")
public class AllocationRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String warehouseCode;
    private String ownerCode;
    private String categoryCode;
    private String skuCode;
    private String orderType;

    /** 优先级 */
    private Integer priority;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
