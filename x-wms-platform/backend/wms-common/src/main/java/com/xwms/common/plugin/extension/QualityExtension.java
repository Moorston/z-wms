package com.xwms.common.plugin.extension;

import java.util.Map;

import com.xwms.common.plugin.WmsPlugin;

/** 质检扩展点 行业插件可定义质检规则、抽样方案、质检流程 */
public interface QualityExtension extends WmsPlugin {

    /** 是否需要质检（如医药全检、食品抽检） */
    default boolean needQualityCheck(Map<String, Object> context) {
        return false;
    }

    /** 质检类型（IQC来料检/过程检/出库检/冷链温度检） */
    default String getQualityType(Map<String, Object> context) {
        return "IQC";
    }

    /** 抽样方案（如医药AQL抽样、食品百分比抽样） */
    default ExtensionResult getSamplingPlan(Map<String, Object> context) {
        return null;
    }

    /** 质检项目（如医药性状/鉴别/含量、冷链温度记录） */
    default ExtensionResult getQualityItems(Map<String, Object> context) {
        return null;
    }

    /** 质检结果处理（如不合格品隔离、冷链断链处理） */
    default void onQualityResult(Map<String, Object> context) {}
}
