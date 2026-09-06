package com.xwms.core.replenish.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 补货任务 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_replenish_task")
public class ReplenishTask extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 补货任务号 */
    private String taskNo;

    /** 关联规则ID */
    private Long ruleId;

    private String sku;
    private String barcode;
    private String productName;
    private String batchNo;
    private String ownerCode;
    private String warehouseCode;

    /** 源库位(存储区) */
    private String fromLocation;

    /** 目标库位(拣货区) */
    private String toLocation;

    /** 计划补货量 */
    private BigDecimal planQty;

    /** 实际补货量 */
    private BigDecimal actualQty;

    /** 补货类型: NORMAL/URGENT/CROSSDOCK/PRESALE/PERIODIC */
    private String replenishType;

    /** 优先级 1-10 */
    private Integer priority;

    /** 状态: PENDING/ASSIGNED/PICKING/PICKED/PUTAWAYING/COMPLETED/EXCEPTION/CANCELLED */
    private String status;

    /** 触发来源: AUTO/MANUAL/SHORTAGE/SCHEDULE/PRESALE */
    private String triggerSource;

    /** 分配的作业员 */
    private String assignee;

    private LocalDateTime assignTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    /** 异常原因 */
    private String exceptionReason;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
