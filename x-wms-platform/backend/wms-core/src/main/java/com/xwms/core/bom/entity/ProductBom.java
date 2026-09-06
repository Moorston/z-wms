package com.xwms.core.bom.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 产品BOM主表 定义父件与子件的组装关系，用于组件扫描收货（子件→父件） */
@Data
@TableName("wms_product_bom")
public class ProductBom {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** BOM编码 */
    private String bomCode;

    /** BOM名称 */
    private String bomName;

    /** 父件商品编码 */
    private String parentSkuCode;

    /** 父件商品名称 */
    private String parentSkuName;

    /** 父件规格 */
    private String parentSpec;

    /** 父件单位 */
    private String parentUnit;

    /** 父件数量（一个BOM组合产出的父件数量） */
    private BigDecimal parentQty;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** BOM类型：ASSEMBLY组装/KIT套装/PROMOTION促销组合 */
    private String bomType;

    /** 状态：ENABLED启用/DISABLED禁用 */
    private String status;

    /** 版本号 */
    private String version;

    /** 是否默认版本：Y/N */
    private String isDefault;

    /** 生效日期 */
    private LocalDateTime effectiveDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

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
