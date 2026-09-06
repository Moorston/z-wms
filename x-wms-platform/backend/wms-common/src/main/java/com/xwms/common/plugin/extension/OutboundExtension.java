package com.xwms.common.plugin.extension;

import java.util.Map;

import com.xwms.common.plugin.WmsPlugin;

/** 出库扩展点 行业插件可拦截出库全流程：订单→分配→波次→拣货→复核→发运 */
public interface OutboundExtension extends WmsPlugin {

    /** 订单创建前校验（如医药处方校验、电商预售校验） */
    default ExtensionResult beforeOrderCreate(Map<String, Object> context) {
        return null;
    }

    /** 库存分配前校验（如医药近效期先出、冷链指定批次） */
    default ExtensionResult beforeAllocation(Map<String, Object> context) {
        return null;
    }

    /** 分配结果修正（如强制指定批号、冷链库位） */
    default ExtensionResult modifyAllocation(Map<String, Object> context) {
        return null;
    }

    /** 拣货校验（如医药双人复核、冷链温度确认） */
    default ExtensionResult beforePick(Map<String, Object> context) {
        return null;
    }

    /** 复核校验（如医药批号核对、冷链装箱温度） */
    default ExtensionResult beforeReview(Map<String, Object> context) {
        return null;
    }

    /** 发运前校验（如冷链运输温度记录、医药随货同行单） */
    default ExtensionResult beforeShip(Map<String, Object> context) {
        return null;
    }
}
