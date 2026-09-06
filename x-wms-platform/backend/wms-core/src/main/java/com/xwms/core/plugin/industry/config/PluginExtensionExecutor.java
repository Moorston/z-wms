package com.xwms.core.plugin.industry.config;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.xwms.common.plugin.extension.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 行业插件扩展点执行器 统一调用所有已启用的行业插件扩展点，按优先级执行 核心流程： 1. 从Spring容器获取所有实现了某扩展点接口的Bean 2. 按配置过滤已启用的插件 3. 按优先级排序
 * 4. 依次执行，第一个非null结果即返回（责任链模式） 5. 全部返回null则返回pass（不拦截）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PluginExtensionExecutor {

    private final ApplicationContext applicationContext;
    private final IndustryPluginConfig pluginConfig;

    /** 执行入库扩展点 */
    public ExtensionResult executeInbound(String method, Map<String, Object> context) {
        List<InboundExtension> plugins = getEnabledPlugins(InboundExtension.class);
        for (InboundExtension plugin : plugins) {
            try {
                ExtensionResult result =
                        switch (method) {
                            case "beforeAsnCreate" -> plugin.beforeAsnCreate(context);
                            case "beforeReceive" -> plugin.beforeReceive(context);
                            case "suggestPutaway" -> plugin.suggestPutaway(context);
                            default -> null;
                        };
                if (result != null && result.isIntercepted()) {
                    log.debug(
                            "入库扩展点拦截: plugin={}, method={}, msg={}",
                            plugin.getPluginId(),
                            method,
                            result.getMessage());
                    return result;
                }
            } catch (Exception e) {
                log.error("入库扩展点执行异常: plugin={}, method={}", plugin.getPluginId(), method, e);
            }
        }
        return ExtensionResult.pass();
    }

    /** 执行出库扩展点 */
    public ExtensionResult executeOutbound(String method, Map<String, Object> context) {
        List<OutboundExtension> plugins = getEnabledPlugins(OutboundExtension.class);
        for (OutboundExtension plugin : plugins) {
            try {
                ExtensionResult result =
                        switch (method) {
                            case "beforeOrderCreate" -> plugin.beforeOrderCreate(context);
                            case "beforeAllocation" -> plugin.beforeAllocation(context);
                            case "modifyAllocation" -> plugin.modifyAllocation(context);
                            case "beforePick" -> plugin.beforePick(context);
                            case "beforeReview" -> plugin.beforeReview(context);
                            case "beforeShip" -> plugin.beforeShip(context);
                            default -> null;
                        };
                if (result != null && result.isIntercepted()) {
                    log.debug("出库扩展点拦截: plugin={}, method={}", plugin.getPluginId(), method);
                    return result;
                }
            } catch (Exception e) {
                log.error("出库扩展点执行异常: plugin={}, method={}", plugin.getPluginId(), method, e);
            }
        }
        return ExtensionResult.pass();
    }

    /** 执行库存扩展点 */
    public ExtensionResult executeInventory(String method, Map<String, Object> context) {
        List<InventoryExtension> plugins = getEnabledPlugins(InventoryExtension.class);
        for (InventoryExtension plugin : plugins) {
            try {
                ExtensionResult result =
                        switch (method) {
                            case "beforeDeduct" -> plugin.beforeDeduct(context);
                            case "beforeAllocate" -> plugin.beforeAllocate(context);
                            case "beforeTransfer" -> plugin.beforeTransfer(context);
                            case "beforeStocktake" -> plugin.beforeStocktake(context);
                            default -> null;
                        };
                if (result != null && result.isIntercepted()) {
                    return result;
                }
            } catch (Exception e) {
                log.error("库存扩展点执行异常: plugin={}, method={}", plugin.getPluginId(), method, e);
            }
        }
        return ExtensionResult.pass();
    }

    /** 执行质检扩展点 */
    public ExtensionResult executeQuality(String method, Map<String, Object> context) {
        List<QualityExtension> plugins = getEnabledPlugins(QualityExtension.class);
        for (QualityExtension plugin : plugins) {
            try {
                ExtensionResult result =
                        switch (method) {
                            case "getSamplingPlan" -> plugin.getSamplingPlan(context);
                            case "getQualityItems" -> plugin.getQualityItems(context);
                            default -> null;
                        };
                if (result != null && result.isIntercepted()) {
                    return result;
                }
            } catch (Exception e) {
                log.error("质检扩展点执行异常: plugin={}, method={}", plugin.getPluginId(), method, e);
            }
        }
        return ExtensionResult.pass();
    }

    /** 获取已启用的插件（按优先级排序） */
    private <T extends com.xwms.common.plugin.WmsPlugin> List<T> getEnabledPlugins(Class<T> type) {
        if (!pluginConfig.isEnabled()) return List.of();
        return applicationContext.getBeansOfType(type).values().stream()
                .filter(p -> pluginConfig.isPluginEnabled(p.getPluginId()))
                .sorted(
                        (a, b) ->
                                Integer.compare(
                                        pluginConfig.getPluginPriority(
                                                b.getPluginId(), b.getPriority()),
                                        pluginConfig.getPluginPriority(
                                                a.getPluginId(), a.getPriority())))
                .collect(Collectors.toList());
    }
}
