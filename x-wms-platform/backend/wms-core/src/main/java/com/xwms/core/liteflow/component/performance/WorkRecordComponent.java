package com.xwms.core.liteflow.component.performance;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.performance.entity.WorkRecord;
import com.xwms.core.performance.service.PerformanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow绩效组件 - 作业完成后自动记录作业绩效 入库/出库/盘点等作业完成后, 自动记录作业人员的作业量和耗时 */
@Slf4j
@LiteflowComponent("workRecord")
@RequiredArgsConstructor
public class WorkRecordComponent extends NodeComponent {

    private final PerformanceService performanceService;

    @Override
    public void process() {
        WorkRecord record = this.getContextBean(WorkRecord.class);
        if (record == null || record.getEmployeeId() == null) {
            log.info("无作业记录或人员ID, 跳过");
            return;
        }
        try {
            performanceService.createWorkRecord(record);
            log.info(
                    "作业绩效记录: 人员={}, 类型={}, 数量={}",
                    record.getEmployeeNo(),
                    record.getWorkType(),
                    record.getQuantity());
        } catch (Exception e) {
            log.error("作业绩效记录失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
