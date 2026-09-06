package com.xwms.core.expiry.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 收货人效期管理表 不同收货人对效期要求不同，例如A超市要求剩余6个月，B超市要求剩余1年 */
@Data
@TableName("wms_customer_expiry")
public class CustomerExpiry {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收货人编码 */
    private String customerCode;

    /** 收货人名称 */
    private String customerName;

    /** 商品编码（为空表示该收货人所有商品通用） */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 出库效期要求（出库日到失效日期有效天数） */
    private Integer outboundExpiryDays;

    /** 入库效期要求（入库后安全存放最大天数） */
    private Integer inboundExpiryDays;

    /** 最小剩余效期（天数） */
    private Integer minRemainingDays;

    /** 预警提前天数 */
    private Integer warningDays;

    /** 效期不足处理方式：REJECT拒绝/WARNING预警/ALLOW允许 */
    private String insufficientHandleType;

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
