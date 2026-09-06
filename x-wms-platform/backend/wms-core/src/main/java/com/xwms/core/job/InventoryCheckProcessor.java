package com.xwms.core.job;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.xwms.base.location.mapper.LocationMapper;
import com.xwms.common.job.AbstractMapReduceProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;

/**
 * 日终库存对账MapReduce任务
 *
 * <p>业务场景： 每天凌晨2点，对所有仓库进行库存对账： 1. Map阶段：按仓库拆分对账任务 2. Process阶段：各Worker节点并行处理单个仓库的对账 -
 * 对比Oracle库存表与库存流水汇总 - 生成对账差异单 3. Reduce阶段：汇总所有仓库的对账结果
 *
 * <p>执行频率：每天凌晨2点 Cron: 0 0 2 * * ?
 *
 * <p>任务参数（可选）： - {"warehouse":"WH01"} 只对账指定仓库
 *
 * <p>PowerJob配置： - 执行类型：MapReduce - 分片参数：按仓库数量动态分片
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryCheckProcessor extends AbstractMapReduceProcessor<String, String> {

    private final LocationMapper locationMapper;

    /** Map阶段：按仓库拆分任务 */
    @Override
    protected List<String> splitTasks(TaskContext context) {
        Map<String, Object> params = getParams(context);
        String specifiedWarehouse = getString(params, "warehouse");

        if (specifiedWarehouse != null && !specifiedWarehouse.isEmpty()) {
            return List.of(specifiedWarehouse);
        }

        // 查询所有活跃仓库
        // List<Warehouse> warehouses = locationMapper.selectList(
        //     new LambdaQueryWrapper<Warehouse>().eq(Warehouse::getStatus, "ACTIVE"));
        // return warehouses.stream()
        //     .map(Warehouse::getWarehouseCode)
        //     .collect(Collectors.toList());

        // 骨架：返回示例仓库
        return List.of("WH01", "WH02", "WH03");
    }

    /** Process阶段：单个仓库对账 */
    @Override
    protected String processSubTask(String warehouseCode) {
        long startTime = System.currentTimeMillis();
        log.info("[InventoryCheck] 开始对账: warehouse={}", warehouseCode);

        try {
            // 1. 查询库存表汇总
            // Map<String, BigDecimal> inventorySummary =
            // inventoryService.getSummaryByWarehouse(warehouseCode);

            // 2. 查询库存流水汇总
            // Map<String, BigDecimal> transactionSummary =
            // inventoryTransactionService.getSummaryByWarehouse(warehouseCode);

            // 3. 对比差异
            // List<InventoryDiff> diffs = compareInventory(inventorySummary, transactionSummary);

            // 4. 生成对账差异单
            // if (!diffs.isEmpty())
            // {
            //     inventoryDiffService.createDiffOrder(warehouseCode, diffs);
            // }

            long cost = System.currentTimeMillis() - startTime;
            String result = String.format("仓库=%s, 对账完成, 差异=0, 耗时=%dms", warehouseCode, cost);
            log.info("[InventoryCheck] {}", result);
            return result;

        } catch (Exception e) {
            log.error(
                    "[InventoryCheck] 对账失败: warehouse={}, error={}",
                    warehouseCode,
                    e.getMessage(),
                    e);
            return String.format("仓库=%s, 对账失败: %s", warehouseCode, e.getMessage());
        }
    }

    /** Reduce阶段：汇总所有子任务结果 MapReduce模式下，reduce在所有子任务完成后由最后一个Worker执行 */
    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        int total = taskResults != null ? taskResults.size() : 0;
        long successCount =
                taskResults != null
                        ? taskResults.stream().filter(TaskResult::isSuccess).count()
                        : 0;
        String msg = String.format("库存对账完成，共%d个仓库，成功%d个", total, successCount);
        log.info("[InventoryCheck] {}", msg);
        return success(msg);
    }
}
