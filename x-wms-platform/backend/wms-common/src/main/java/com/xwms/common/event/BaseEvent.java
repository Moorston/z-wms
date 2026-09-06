package com.xwms.common.event;

import java.time.LocalDateTime;

import lombok.Data;

/** 领域事件基类 所有WMS业务事件继承此类，用于事件溯源和异步通知 */
@Data
public abstract class BaseEvent {
    /** 事件ID */
    private String eventId;

    /** 事件类型 */
    private String eventType;

    /** 聚合根ID（如订单号/入库单号） */
    private String aggregateId;

    /** 事件发生时间 */
    private LocalDateTime eventTime = LocalDateTime.now();

    /** 操作人 */
    private String operator;

    /** 仓库 */
    private String warehouse;

    /** 事件版本（乐观锁） */
    private Long version;

    /** 扩展数据（JSON） */
    private String payload;

    protected BaseEvent(String eventType, String aggregateId) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.eventType = eventType;
        this.aggregateId = aggregateId;
    }
}
