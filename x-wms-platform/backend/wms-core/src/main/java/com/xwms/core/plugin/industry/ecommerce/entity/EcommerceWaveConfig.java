package com.xwms.core.plugin.industry.ecommerce.entity;

import java.time.LocalDateTime;
import java.time.LocalTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 电商波次配置 针对电商大促场景的波次策略配置 */
@Data
@TableName("wms_ecommerce_wave_config")
public class EcommerceWaveConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置编号 */
    private String configCode;

    /** 配置名称 */
    private String configName;

    /** 店铺编码 */
    private String shopCode;

    /** 平台编码 */
    private String platformCode;

    /** 活动类型: NORMAL日常/FLASH_SALE秒杀/BIG_PROMOTION大促/PRE_SALE预售/RETURN退货 */
    private String activityType;

    /** 波次类型: TIME_BASED按时间/ORDER_COUNT按订单数量/EXPRESS按快递/PRIORITY按优先级 */
    private String waveType;

    /** 时间间隔(分钟)，按时间波次时使用 */
    private Integer timeInterval;

    /** 最大订单数，按订单数量波次时使用 */
    private Integer maxOrderCount;

    /** 最大SKU数 */
    private Integer maxSkuCount;

    /** 最大件数 */
    private Integer maxItemCount;

    /** 波次开始时间 */
    private LocalTime waveStartTime;

    /** 波次结束时间 */
    private LocalTime waveEndTime;

    /** 拣货模式: PICK_TO_ORDER摘果式/SORT_TO_ORDER播种式/BATCH_PICK批量拣货/REGION_PICK分区拣货 */
    private String pickMode;

    /** 是否合并波次: Y是/N否 */
    private String allowMerge;

    /** 优先级(数字越小优先级越高) */
    private Integer priority;

    /** 是否自动释放波次: Y是/N否 */
    private String autoRelease;

    /** 波次超时时间(分钟) */
    private Integer timeoutMinutes;

    /** 是否启用: Y是/N否 */
    private String enabled;

    /** 生效开始日期 */
    private LocalDateTime effectiveStartDate;

    /** 生效结束日期 */
    private LocalDateTime effectiveEndDate;

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
