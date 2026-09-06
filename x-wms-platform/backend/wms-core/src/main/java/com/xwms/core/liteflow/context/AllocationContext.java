package com.xwms.core.liteflow.context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/** 库存分配流程上下文 出库单创建后，按规则从哪个库位、哪个批次分配库存 */
@Data
public class AllocationContext {

    /** 出库单号 */
    private String orderNo;

    /** SKU编码 */
    private String sku;

    /** 仓库编码 */
    private String warehouse;

    /** 需求数量 */
    private BigDecimal requiredQty;

    /** 分配策略：FIFO/FEFO/LIFO/指定批次/就近库位 */
    private String strategy;

    /** 货主编码 */
    private String ownerCode;

    /** 候选库位列表（经过规则过滤后） */
    private List<Map<String, Object>> candidateLocations = new ArrayList<>();

    /** 分配结果：库位 → 批次 → 分配数量 */
    private Map<String, Map<String, BigDecimal>> allocationResult = new HashMap<>();

    /** 已分配总量 */
    private BigDecimal allocatedQty = BigDecimal.ZERO;

    /** Redis预占结果 */
    private boolean redisReserved = false;

    /** Oracle预占结果 */
    private boolean oracleReserved = false;

    /** 执行结果 */
    private boolean success = true;

    /** 错误信息 */
    private String errorMessage;

    /** 添加分配结果 */
    public void addAllocation(String location, String batch, BigDecimal qty) {
        allocationResult
                .computeIfAbsent(location, k -> new HashMap<>())
                .merge(batch, qty, BigDecimal::add);
        allocatedQty = allocatedQty.add(qty);
    }

    /** 标记失败 */
    public void markFailed(String message) {
        this.success = false;
        this.errorMessage = message;
    }
}
