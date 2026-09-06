package com.xwms.analytics.job;

import org.springframework.stereotype.Component;

import com.xwms.analytics.sync.DataSyncService;
import com.xwms.common.job.AbstractPowerJobProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;

/**
 * 数据同步定时任务（Oracle → ClickHouse）
 *
 * <p>业务场景： 1. 增量同步：每5分钟同步库存/订单/作业数据到ClickHouse 2. 全量同步：每天凌晨2点全量同步历史数据 3. 同步完成后触发KPI预计算
 *
 * <p>执行频率： - 增量：0 * /5 * * * ? （每5分钟，cron 表达式 * /5） - 全量：0 0 2 * * ? （每天凌晨2点）
 *
 * <p>任务参数： - {"mode":"INCREMENTAL"} 增量同步 - {"mode":"FULL"} 全量同步
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSyncProcessor extends AbstractPowerJobProcessor {

    private final DataSyncService dataSyncService;

    @Override
    protected ProcessResult doProcess(TaskContext context) {
        var params = getParams(context);
        String mode = getString(params, "mode", "INCREMENTAL");

        long startTime = System.currentTimeMillis();

        try {
            if ("FULL".equalsIgnoreCase(mode)) {
                // 全量同步
                log.info("[DataSync] 开始全量同步...");
                dataSyncService.fullSync();
                long cost = System.currentTimeMillis() - startTime;
                return success(String.format("全量同步完成, 耗时=%dms", cost));
            } else {
                // 增量同步
                log.info("[DataSync] 开始增量同步...");
                int syncCount = dataSyncService.incrementalSync();
                long cost = System.currentTimeMillis() - startTime;
                return success(String.format("增量同步完成, 同步记录=%d, 耗时=%dms", syncCount, cost));
            }
        } catch (Exception e) {
            log.error("[DataSync] 数据同步异常: mode={}, error={}", mode, e.getMessage(), e);
            return fail("数据同步异常: " + e.getMessage());
        }
    }
}
