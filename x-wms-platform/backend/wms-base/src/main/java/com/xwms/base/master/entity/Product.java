package com.xwms.base.master.entity;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 商品档案实体 SKU级别的商品主数据 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_product")
public class Product extends BaseEntity {
    /** SKU编码 */
    private String sku;

    /** 商品名称 */
    private String productName;

    /** 货主 */
    private String ownerCode;

    /** 条码 */
    private String barcode;

    /** 分类 */
    private String category;

    /** 品牌 */
    private String brand;

    /** 规格 */
    private String spec;

    /** 单位 */
    private String unit;

    /** 重量(kg) */
    private BigDecimal weight;

    /** 体积(m³) */
    private BigDecimal volume;

    /** 存储温区 */
    private String temperatureZone;

    /** 保质期(天) */
    private Integer shelfLifeDays;

    /** 是否批次管理 */
    private Boolean batchManaged;

    /** 是否序列号管理 */
    private Boolean serialManaged;

    /** 上架规则编码 */
    private String putawayRule;

    /** 分配规则编码 */
    private String allocationRule;

    /** 周转规则编码 */
    private String rotationRule;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 扩展属性（JSON） */
    private String productAttrs;
}
