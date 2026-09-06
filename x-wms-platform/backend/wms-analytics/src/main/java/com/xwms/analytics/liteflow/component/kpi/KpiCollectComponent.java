package com.xwms.analytics.liteflow.component.kpi;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.analytics.kpi.service.KpiService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow KPI数据采集组件 - 业务流程中自动采集KPI 作业完成后自动记录KPI数据(拣货效率/入库效率等) */
@Slf4j
@LiteflowComponent("kpiCollect")
@RequiredArgsConstructor
public class KpiCollectComponent extends NodeComponent {

    private final KpiService kpiService;

    @Override
    public void process() {
        @SuppressWarnings("unchecked")
        Map<String, Object> context = this.getContextBean(Map.class);
        if (context == null || context.get("kpiCode") == null) {
            log.info("无KPI上下文, 跳过");
            return;
        }
        try {
            String kpiCode = context.get("kpiCode").toString();
            LocalDate statDate =
                    context.get("statDate") != null
                            ? (LocalDate) context.get("statDate")
                            : LocalDate.now();
            String warehouseCode =
                    context.get("warehouseCode") != null
                            ? context.get("warehouseCode").toString()
                            : null;
            String department =
                    context.get("department") != null ? context.get("department").toString() : null;
            String employeeId =
                    context.get("employeeId") != null ? context.get("employeeId").toString() : null;
            BigDecimal actualValue =
                    context.get("actualValue") != null
                            ? new BigDecimal(context.get("actualValue").toString())
                            : BigDecimal.ZERO;
            String dataDetail =
                    context.get("dataDetail") != null ? context.get("dataDetail").toString() : null;

            kpiService.collectKpiData(
                    kpiCode,
                    statDate,
                    warehouseCode,
                    department,
                    employeeId,
                    actualValue,
                    dataDetail);
            log.info("流程KPI采集: {}={}", kpiCode, actualValue);
        } catch (Exception e) {
            log.error("流程KPI采集失败: {}", e.getMessage());
        }
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
