package com.xwms.core.putaway.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 上架领域事件基类 所有上架相关事件都继承此类，通过Kafka发布 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PutawayEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 事件ID（唯一） */
    private String eventId;

    /** 事件类型：TASK_CREATED任务生成/TASK_COMPLETED上架完成/TASK_EXCEPTION异常/TASK_OVERRIDE人工覆盖 */
    private String eventType;

    /** 事件时间 */
    private LocalDateTime eventTime;

    /** 上架任务号 */
    private String taskNo;

    /** 任务明细号 */
    private String detailNo;

    /** 仓库编码 */
    private String warehouseCode;

    /** 货主编码 */
    private String ownerCode;

    /** 商品编码 */
    private String skuCode;

    /** 批次号 */
    private String batchNo;

    /** 源库位 */
    private String sourceLocation;

    /** 目标库位 */
    private String targetLocation;

    /** 上架数量 */
    private BigDecimal putawayQty;

    /** 操作人 */
    private String operator;

    /** 异常类型（异常事件时） */
    private String exceptionType;

    /** 原因代码（异常/覆盖事件时） */
    private String reasonCode;

    /** 备注 */
    private String remark;

    /** 事件来源服务 */
    private String sourceService;

    /** TraceId（链路追踪） */
    private String traceId;
}
