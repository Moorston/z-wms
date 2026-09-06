package com.xwms.integration.adapter;

import java.util.Map;

import com.xwms.common.plugin.WmsPlugin;

/** 外部系统集成适配器接口 所有ERP/TMS/WCS/电商/快递适配器实现此接口 通过SPI插件机制加载，新系统对接只需开发适配器 */
public interface IntegrationAdapter extends WmsPlugin {
    /** 系统类型：ERP / TMS / WCS / ECOMMERCE / EXPRESS */
    String getSystemType();

    /** 推送订单到外部系统 */
    Map<String, Object> pushOrder(Map<String, Object> order);

    /** 从外部系统拉取订单 */
    Map<String, Object> pullOrder(Map<String, Object> params);

    /** 健康检查 */
    boolean healthCheck();
}
