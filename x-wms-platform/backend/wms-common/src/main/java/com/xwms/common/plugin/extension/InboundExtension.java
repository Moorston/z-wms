package com.xwms.common.plugin.extension;

import java.util.Map;

import com.xwms.common.plugin.WmsPlugin;

/** 入库扩展点 行业插件可拦截入库全流程：ASN校验→收货→质检→上架 每个方法返回null表示不拦截，返回ExtensionResult表示拦截并给出处理结果 */
public interface InboundExtension extends WmsPlugin {

    /** ASN创建/接收前校验（如医药首营审核、冷链资质校验） */
    default ExtensionResult beforeAsnCreate(Map<String, Object> context) {
        return null;
    }

    /** 收货校验（如批号效期必填、冷链温度记录） */
    default ExtensionResult beforeReceive(Map<String, Object> context) {
        return null;
    }

    /** 收货后处理（如生成温湿度记录、触发首营审批） */
    default void afterReceive(Map<String, Object> context) {}

    /** 上架建议（如医药按批号分区、冷链指定冷库位） */
    default ExtensionResult suggestPutaway(Map<String, Object> context) {
        return null;
    }

    /** 上架后处理（如更新批次效期、生成养护计划） */
    default void afterPutaway(Map<String, Object> context) {}
}
