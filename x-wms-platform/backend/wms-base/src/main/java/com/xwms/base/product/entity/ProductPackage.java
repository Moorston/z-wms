package com.xwms.base.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 产品包装 */
@Data
@TableName("wms_product_package")
public class ProductPackage {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String packageCode;
    private String packageName;
    private String skuCode;

    /** 包装类型: BOX/PALLET/CARTON/BAG/PIECE */
    private String packageType;

    /** 包装内数量 */
    private BigDecimal quantity;

    private BigDecimal weight;
    private BigDecimal volume;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;

    /** 最大堆码层数 */
    private Integer maxStack;

    /** 是否默认包装 */
    private Integer isDefault;

    private String status;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
