package com.xwms.base.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 产品档案 */
@Data
@TableName("wms_product")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String skuCode;
    private String skuName;
    private String skuShortName;
    private String categoryCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String brand;
    private String spec;
    private String model;

    /** 基本单位 */
    private String unit;

    /** 辅助单位 */
    private String secondaryUnit;

    /** 换算率 */
    private BigDecimal convertRate;

    /** 重量(kg) */
    private BigDecimal weight;

    /** 体积(m3) */
    private BigDecimal volume;

    /** 长(cm) */
    private BigDecimal length;

    /** 宽(cm) */
    private BigDecimal width;

    /** 高(cm) */
    private BigDecimal height;

    /** 标准价 */
    private BigDecimal price;

    /** 成本价 */
    private BigDecimal cost;

    /** 保质期 */
    private Integer shelfLife;

    /** 保质期单位: DAY/MONTH/YEAR */
    private String shelfLifeUnit;

    /** 存储温度要求 */
    private String storageTemp;

    /** 是否批次管理 */
    private Integer isBatchMgmt;

    /** 是否序列号管理 */
    private Integer isSerialMgmt;

    /** 是否易碎 */
    private Integer isFragile;

    /** 是否危险品 */
    private Integer isHazardous;

    /** 状态: ACTIVE/DISABLED */
    private String status;

    /** 产品扩展属性(JSON) */
    private String productAttrs;

    private String imageUrl;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
