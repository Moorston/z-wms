package com.xwms.core.plugin.industry.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 秒杀活动 电商平台限时秒杀活动管理 */
@Data
@TableName("wms_ecommerce_flash_sale")
public class FlashSaleActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 活动编号 */
    private String activityNo;

    /** 活动名称 */
    private String activityName;

    /** 店铺编码 */
    private String shopCode;

    /** 平台编码 */
    private String platformCode;

    /** 活动类型: FLASH_SALE秒杀/GROUP_BUY拼团/LIMITED_DISCOUNT限时折扣/PRE_SALE预售 */
    private String activityType;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 活动库存数量 */
    private BigDecimal activityStock;

    /** 已售数量 */
    private BigDecimal soldQty;

    /** 剩余数量 */
    private BigDecimal remainingQty;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 活动价 */
    private BigDecimal activityPrice;

    /** 折扣率(%) */
    private BigDecimal discountRate;

    /** 每人限购数量 */
    private BigDecimal limitPerPerson;

    /** 活动开始时间 */
    private LocalDateTime startTime;

    /** 活动结束时间 */
    private LocalDateTime endTime;

    /** 预热开始时间 */
    private LocalDateTime preheatStartTime;

    /** 活动状态: DRAFT草稿/PENDING待开始/PREHEAT预热中/ACTIVE进行中/ENDED已结束/CANCELLED已取消 */
    private String status;

    /** 库存锁定状态: NOT_LOCKED未锁定/LOCKED已锁定/RELEASED已释放 */
    private String stockLockStatus;

    /** 锁定库存时间 */
    private LocalDateTime lockTime;

    /** 关联波次号 */
    private String waveNo;

    /** 优先级(数字越小优先级越高) */
    private Integer priority;

    /** 自动发货: Y是/N否 */
    private String autoShip;

    /** 发货截止时间 */
    private LocalDateTime shipDeadline;

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
