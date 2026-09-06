package com.xwms.core.rotation.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 周转规则 */
@Data
@TableName("wms_rotation_rule")
public class RotationRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 适用SKU(空表示所有) */
    private String skuCode;

    /** 适用品类 */
    private String categoryCode;

    /** 适用货主 */
    private String ownerCode;

    /** 周转类型: FIFO/FEFO/LIFO/FIFO_FEFO */
    private String rotationType;

    /** 排序字段: PRODUCTION_DATE/EXPIRE_DATE/RECEIVE_DATE/BATCH_NO */
    private String sortField;

    /** 排序顺序: ASC/DESC */
    private String sortOrder;

    private Integer priority;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
