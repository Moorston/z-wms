package com.xwms.core.liteflow.context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xwms.core.operation.entity.WorkTask;
import com.xwms.core.outbound.entity.OutboundOrder;
import com.xwms.core.outbound.entity.Wave;

import lombok.Data;

/** 波次执行流程上下文 在LiteFlow组件间传递数据，每个流程实例独立上下文 */
@Data
public class WaveExecuteContext {

    /** 波次ID */
    private Long waveId;

    /** 波次编号 */
    private String waveNo;

    /** 波次实体 */
    private Wave wave;

    /** 待处理出库单列表 */
    private List<OutboundOrder> orders = new ArrayList<>();

    /** 生成的作业任务列表 */
    private List<WorkTask> workTasks = new ArrayList<>();

    /** 拣货路径规划结果 */
    private List<String> pickPath = new ArrayList<>();

    /** 库存预占结果：orderNo → allocatedQty */
    private Map<String, BigDecimal> allocationResult = new HashMap<>();

    /** 行业类型（决定流程分支） */
    private String industryType;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouse;

    /** 执行结果 */
    private boolean success = true;

    /** 错误信息 */
    private String errorMessage;

    /** 扩展参数 */
    private Map<String, Object> extParams = new HashMap<>();

    /** 添加出库单 */
    public void addOrder(OutboundOrder order) {
        this.orders.add(order);
    }

    /** 添加作业任务 */
    public void addWorkTask(WorkTask task) {
        this.workTasks.add(task);
    }

    /** 设置分配结果 */
    public void putAllocation(String orderNo, BigDecimal qty) {
        this.allocationResult.put(orderNo, qty);
    }

    /** 标记失败 */
    public void markFailed(String message) {
        this.success = false;
        this.errorMessage = message;
    }
}
