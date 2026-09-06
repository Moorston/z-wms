package com.xwms.core.liteflow.component.allocation;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.liteflow.context.AllocationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Oracle预占组件 更新Oracle库存表的allocated_qty字段 SQL: UPDATE wms_inventory SET allocated_qty =
 * allocated_qty + ? WHERE sku=? AND warehouse=? AND location=? AND batch_no=? AND qty -
 * allocated_qty >= ?
 */
@Slf4j
@LiteflowComponent("oracleReserve")
public class OracleReserveComponent extends NodeComponent {

    @Override
    public void process() {
        AllocationContext context = this.getContextBean(AllocationContext.class);

        log.info("[库存分配] Oracle预占开始: allocated={}", context.getAllocatedQty());

        // TODO: Oracle原子预占（乐观锁保证并发安全）
        // for (entry : allocationResult)
        // {
        //     int rows = inventoryMapper.reserve(sku, warehouse, location, batch, qty);
        //     if (rows == 0) throw new BizException("预占失败");
        // }

        context.setOracleReserved(true);
        log.info("[库存分配] Oracle预占完成");
    }
}
