package com.xwms.common.plugin.extension;

import java.util.Map;

import com.xwms.common.plugin.WmsPlugin;

/** 库存扩展点 行业插件可拦截库存操作：扣减→预占→调拨→盘点→预警 */
public interface InventoryExtension extends WmsPlugin {

    /** 库存扣减前校验（如医药批号效期、冷链在库温度） */
    default ExtensionResult beforeDeduct(Map<String, Object> context) {
        return null;
    }

    /** 库存预占前校验 */
    default ExtensionResult beforeAllocate(Map<String, Object> context) {
        return null;
    }

    /** 库存调拨校验（如医药跨仓调拨审批、冷链运输温度） */
    default ExtensionResult beforeTransfer(Map<String, Object> context) {
        return null;
    }

    /** 库存预警（如医药近效期预警、冷链温度异常预警） */
    default void onInventoryAlert(Map<String, Object> context) {}

    /** 盘点校验（如医药特殊药品双人盘点） */
    default ExtensionResult beforeStocktake(Map<String, Object> context) {
        return null;
    }
}
