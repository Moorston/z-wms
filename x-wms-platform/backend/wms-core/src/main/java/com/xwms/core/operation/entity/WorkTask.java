package com.xwms.core.operation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 作业任务实体 PDA/工作站执行的最小作业单元 类型：收货/上架/拣货/复核/打包/发运/盘点/移库 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_work_task")
public class WorkTask extends BaseEntity {
    /** 任务号 */
    private String taskNo;

    /** 任务类型：RECEIVE/PUTAWAY/PICK/REVIEW/PACK/SHIP/STOCKTAKE/TRANSFER */
    private String taskType;

    /** 仓库 */
    private String warehouse;

    /** 货主（租户隔离） */
    private String ownerCode;

    /** 状态：PENDING/PROCESSING/COMPLETED/CANCELLED/EXCEPTION */
    private String status;

    /** 优先级：1-9，越大越优先 */
    private Integer priority;

    /** 关联单号（入库单/出库单/波次号） */
    private String referenceNo;

    /** SKU */
    private String sku;

    /** 批号 */
    private String batchNo;

    /** 源库位 */
    private String fromLocation;

    /** 目标库位 */
    private String toLocation;

    /** 计划数量 */
    private BigDecimal planQty;

    /** 实际数量 */
    private BigDecimal actualQty;

    /** 执行人 */
    private String operator;

    /** 执行设备（PDA编号/工作站） */
    private String deviceId;

    /** 计划开始时间 */
    private LocalDateTime plannedStartTime;

    /** 实际开始时间 */
    private LocalDateTime actualStartTime;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 异常原因 */
    private String exceptionReason;

    /** 扩展属性（JSON） */
    private String extAttrs;
}
