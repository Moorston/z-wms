package com.xwms.base.product.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 产品分类 */
@Data
@TableName("wms_product_category")
public class ProductCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String categoryCode;
    private String categoryName;
    private String parentCode;
    private Integer categoryLevel;
    private Integer sortOrder;
    private String status;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
