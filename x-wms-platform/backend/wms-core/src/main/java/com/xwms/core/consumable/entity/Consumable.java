package com.xwms.core.consumable.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 耗材主表 维护包装盒、填充物等包装耗材SKU信息 */
@Data
@TableName("wms_consumable")
public class Consumable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 耗材编码 */
    private String consumableCode;

    /** 耗材名称 */
    private String consumableName;

    /** 耗材类型：BOX包装盒/FILLER填充物/TAPE胶带/LABEL标签/BAG包装袋/OTHER其他 */
    private String consumableType;

    /** 规格型号 */
    private String spec;

    /** 单位 */
    private String unit;

    /** 尺寸-长(mm) */
    private BigDecimal length;

    /** 尺寸-宽(mm) */
    private BigDecimal width;

    /** 尺寸-高(mm) */
    private BigDecimal height;

    /** 承重(kg) */
    private BigDecimal loadCapacity;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 安全库存 */
    private BigDecimal safetyStock;

    /** 当前库存 */
    private BigDecimal currentStock;

    /** 库存单位成本 */
    private BigDecimal unitCost;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 存放库位 */
    private String locationCode;

    /** 状态：ENABLED启用/DISABLED禁用 */
    private String status;

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
