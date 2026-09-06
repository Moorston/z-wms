package com.xwms.core.outbound.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 波次实体 将多个出库单按策略分组为波次，提高拣货效率 分组维度：快递/区域/时间/优先级/SKU */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_wave")
public class Wave extends BaseEntity {
    /** 波次号 */
    private String waveNo;

    /** 仓库 */
    private String warehouse;

    /** 货主（租户隔离） */
    private String ownerCode;

    /** 波次类型：NORMAL/PROMO/URGENT */
    private String waveType;

    /** 拣货模式：PTL/SOW/BATCH/ZONE */
    private String pickMode;

    /** 状态：PENDING/PROCESSING/COMPLETED/CANCELLED */
    private String status;

    /** 订单数 */
    private Integer orderCount;

    /** SKU数 */
    private Integer skuCount;

    /** 总件数 */
    private java.math.BigDecimal totalQty;

    /** 拣货员 */
    private String picker;

    /** 计划开始时间 */
    private LocalDateTime plannedStartTime;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 分组策略（JSON） */
    private String groupStrategy;

    /** 路径规划结果（JSON） */
    private String routePlan;
}
