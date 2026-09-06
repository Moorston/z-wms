package com.xwms.base.batch.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 批次属性定义 */
@Data
@TableName("wms_batch_attribute")
public class BatchAttribute {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String attrCode;
    private String attrName;

    /** 属性类型: STRING/NUMBER/DATE/ENUM/BOOLEAN */
    private String attrType;

    /** 属性分类: PRODUCTION/QUALITY/LOGISTICS/REGULATORY */
    private String attrCategory;

    private Integer dataLength;
    private Integer precisionVal;

    /** 枚举值(JSON数组) */
    private String enumValues;

    /** 是否必填 */
    private Integer isRequired;

    /** 是否唯一(如批号) */
    private Integer isUnique;

    /** 是否可搜索 */
    private Integer isSearchable;

    /** 默认值 */
    private String defaultValue;

    /** 校验规则(正则/表达式) */
    private String validationRule;

    private String description;
    private Integer sortOrder;
    private Integer enabled;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
