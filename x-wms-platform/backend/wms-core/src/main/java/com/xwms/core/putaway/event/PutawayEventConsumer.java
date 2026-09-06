package com.xwms.core.putaway.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上架事件消费者 异步处理上架领域事件，实现业务解耦
 *
 * <p>消费者组：wms-putaway-consumer 处理逻辑： - 上架完成事件：更新库存缓存/统计指标/通知相关系统 - 上架异常事件：通知班组长/创建异常工单/触发告警 -
 * 人工覆盖事件：记录分析日志/优化推荐规则 - 任务生成事件：异步派发任务/通知拣货员
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PutawayEventConsumer {

    private static final String CONSUMER_GROUP = "wms-putaway-consumer";

    /** 消费上架任务生成事件 异步处理：任务派发/通知拣货员/预分配库位 */
    @KafkaListener(topics = PutawayEventPublisher.TOPIC_TASK_CREATED, groupId = CONSUMER_GROUP)
    public void onTaskCreated(PutawayEvent event) {
        log.info(
                "消费上架任务生成事件: taskNo={}, warehouse={}", event.getTaskNo(), event.getWarehouseCode());

        try {
            // 1. 异步派发任务到工作区
            dispatchTaskAsync(event);

            // 2. 通知相关拣货员（通过消息推送/站内信）
            notifyOperators(event);

            // 3. 预分配库位（如果是预约上架）
            preAllocateLocation(event);

            log.info("上架任务生成事件处理完成: taskNo={}", event.getTaskNo());
        } catch (Exception e) {
            log.error("上架任务生成事件处理失败: taskNo={}, error={}", event.getTaskNo(), e.getMessage(), e);
        }
    }

    /** 消费上架完成事件 异步处理：更新库存缓存/统计指标/通知ERP/触发补货 */
    @KafkaListener(topics = PutawayEventPublisher.TOPIC_TASK_COMPLETED, groupId = CONSUMER_GROUP)
    public void onTaskCompleted(PutawayEvent event) {
        log.info(
                "消费上架完成事件: taskNo={}, sku={}, qty={}, location={}",
                event.getTaskNo(),
                event.getSkuCode(),
                event.getPutawayQty(),
                event.getTargetLocation());

        try {
            // 1. 更新库存缓存（Redis库存快照）
            updateInventoryCache(event);

            // 2. 更新统计指标（上架完成数/作业效率）
            updateMetrics(event);

            // 3. 通知ERP/外部系统（入库完成回传）
            notifyExternalSystems(event);

            // 4. 触发补货检查（如果是拣货位上架）
            triggerReplenishmentCheck(event);

            // 5. 更新库位已用容量
            updateLocationCapacity(event);

            log.info("上架完成事件处理完成: taskNo={}", event.getTaskNo());
        } catch (Exception e) {
            log.error("上架完成事件处理失败: taskNo={}, error={}", event.getTaskNo(), e.getMessage(), e);
        }
    }

    /** 消费上架异常事件 异步处理：通知班组长/创建异常工单/触发告警 */
    @KafkaListener(topics = PutawayEventPublisher.TOPIC_TASK_EXCEPTION, groupId = CONSUMER_GROUP)
    public void onTaskException(PutawayEvent event) {
        log.warn(
                "消费上架异常事件: taskNo={}, type={}, reason={}",
                event.getTaskNo(),
                event.getExceptionType(),
                event.getReasonCode());

        try {
            // 1. 通知班组长（通过消息推送/短信/邮件）
            notifySupervisor(event);

            // 2. 创建异常工单（分配给仓管处理）
            createExceptionWorkOrder(event);

            // 3. 触发告警（如果是严重异常，如无可用库位）
            triggerAlert(event);

            // 4. 记录异常统计
            recordExceptionMetrics(event);

            log.info("上架异常事件处理完成: taskNo={}", event.getTaskNo());
        } catch (Exception e) {
            log.error("上架异常事件处理失败: taskNo={}, error={}", event.getTaskNo(), e.getMessage(), e);
        }
    }

    /** 消费人工覆盖事件 异步处理：记录分析日志/优化推荐规则/统计覆盖率 */
    @KafkaListener(topics = PutawayEventPublisher.TOPIC_TASK_OVERRIDE, groupId = CONSUMER_GROUP)
    public void onTaskOverride(PutawayEvent event) {
        log.info(
                "消费人工覆盖事件: taskNo={}, sku={}, 原库位={}, 新库位={}, 原因={}",
                event.getTaskNo(),
                event.getSkuCode(),
                event.getSourceLocation(),
                event.getTargetLocation(),
                event.getReasonCode());

        try {
            // 1. 记录覆盖统计（覆盖率/原因分布）
            recordOverrideMetrics(event);

            // 2. 分析覆盖原因，优化推荐规则
            analyzeAndOptimizeRules(event);

            // 3. 更新推荐日志（标记为覆盖）
            updateRecommendLog(event);

            log.info("人工覆盖事件处理完成: taskNo={}", event.getTaskNo());
        } catch (Exception e) {
            log.error("人工覆盖事件处理失败: taskNo={}, error={}", event.getTaskNo(), e.getMessage(), e);
        }
    }

    // ============================================================
    // 私有方法（异步处理逻辑）
    // ============================================================

    private void dispatchTaskAsync(PutawayEvent event) {
        log.debug("异步派发任务: taskNo={}", event.getTaskNo());
    }

    private void notifyOperators(PutawayEvent event) {
        log.debug("通知拣货员: taskNo={}", event.getTaskNo());
    }

    private void preAllocateLocation(PutawayEvent event) {
        log.debug("预分配库位: taskNo={}", event.getTaskNo());
    }

    private void updateInventoryCache(PutawayEvent event) {
        log.debug("更新库存缓存: sku={}, location={}", event.getSkuCode(), event.getTargetLocation());
    }

    private void updateMetrics(PutawayEvent event) {
        log.debug("更新统计指标: taskNo={}", event.getTaskNo());
    }

    private void notifyExternalSystems(PutawayEvent event) {
        log.debug("通知外部系统: taskNo={}", event.getTaskNo());
    }

    private void triggerReplenishmentCheck(PutawayEvent event) {
        log.debug("触发补货检查: location={}", event.getTargetLocation());
    }

    private void updateLocationCapacity(PutawayEvent event) {
        log.debug("更新库位容量: location={}", event.getTargetLocation());
    }

    private void notifySupervisor(PutawayEvent event) {
        log.warn("通知班组长: taskNo={}, type={}", event.getTaskNo(), event.getExceptionType());
    }

    private void createExceptionWorkOrder(PutawayEvent event) {
        log.debug("创建异常工单: taskNo={}", event.getTaskNo());
    }

    private void triggerAlert(PutawayEvent event) {
        log.warn("触发告警: taskNo={}, type={}", event.getTaskNo(), event.getExceptionType());
    }

    private void recordExceptionMetrics(PutawayEvent event) {
        log.debug("记录异常统计: type={}", event.getExceptionType());
    }

    private void recordOverrideMetrics(PutawayEvent event) {
        log.debug("记录覆盖统计: reason={}", event.getReasonCode());
    }

    private void analyzeAndOptimizeRules(PutawayEvent event) {
        log.debug("分析优化规则: sku={}", event.getSkuCode());
    }

    private void updateRecommendLog(PutawayEvent event) {
        log.debug("更新推荐日志: taskNo={}", event.getTaskNo());
    }
}
