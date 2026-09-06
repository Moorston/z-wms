package com.xwms.core.consumable.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 产品包装关联表 产品档案维护耗材SKU代码，包装中对应包装材料选择耗材代码，产品选择对应包装代码 */
@Data
@TableName("wms_product_packaging")
public class ProductPackaging {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 包装代码 */
    private String packagingCode;

    /** 包装名称 */
    private String packagingName;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 耗材编码 */
    private String consumableCode;

    /** 耗材名称 */
    private String consumableName;

    /** 耗材类型 */
    private String consumableType;

    /** 单位产品耗材用量（每件产品消耗的耗材数量） */
    private BigDecimal usagePerUnit;

    /** 包装层级：1=内包装/2=中包装/3=外包装 */
    private Integer packagingLevel;

    /** 是否启用：Y/N */
    private String enabled;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
