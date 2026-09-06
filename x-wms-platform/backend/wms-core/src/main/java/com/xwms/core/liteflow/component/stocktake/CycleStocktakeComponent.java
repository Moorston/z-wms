package com.xwms.core.liteflow.component.stocktake;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;

import com.xwms.core.stocktake.service.StocktakeService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** LiteFlow盘点组件 - 循环盘点定时触发 PowerJob定时调用, 根据ABC分类自动生成循环盘点任务 */
@Slf4j
@LiteflowComponent("cycleStocktake")
@RequiredArgsConstructor
public class CycleStocktakeComponent extends NodeComponent {

    private final StocktakeService stocktakeService;

    @Override
    public void process() {
        // 从上下文获取仓库和ABC分类
        String warehouseCode = this.getContextBean(String.class);
        if (warehouseCode == null) {
            log.info("无仓库信息, 跳过循环盘点");
            return;
        }

        log.info("触发循环盘点: 仓库={}", warehouseCode);
        // TODO: 根据ABC分类生成盘点任务
        // A类: 每月盘点一次
        // B类: 每季度盘点一次
        // C类: 每半年盘点一次
    }

    @Override
    public boolean isAccess() {
        return true;
    }
}
