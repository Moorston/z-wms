package com.xwms.core.consumable.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 耗材扣减记录表 记录入库/出库时耗材的扣减明细 */
@Data
@TableName("wms_consumable_record")
public class ConsumableRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记录单号 */
    private String recordNo;

    /** 业务类型：INBOUND入库/OUTBOUND出库/TRANSFER调拨/ADJUST调整 */
    private String businessType;

    /** 关联单据号（入库单/出库单等） */
    private String refNo;

    /** 关联单据明细号 */
    private String refDetailNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 耗材编码 */
    private String consumableCode;

    /** 耗材名称 */
    private String consumableName;

    /** 耗材类型 */
    private String consumableType;

    /** 包装代码 */
    private String packagingCode;

    /** 产品数量 */
    private BigDecimal productQty;

    /** 单位用量 */
    private BigDecimal usagePerUnit;

    /** 扣减数量 */
    private BigDecimal deductQty;

    /** 扣减前库存 */
    private BigDecimal beforeStock;

    /** 扣减后库存 */
    private BigDecimal afterStock;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
