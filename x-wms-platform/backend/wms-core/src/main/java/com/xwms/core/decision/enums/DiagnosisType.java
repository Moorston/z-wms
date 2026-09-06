package com.xwms.core.decision.enums;

import lombok.Getter;

@Getter
public enum DiagnosisType {
    ACCURACY("ACCURACY", "库存准确率诊断"),
    TURNOVER("TURNOVER", "库存周转率诊断"),
    OBSOLETE("OBSOLETE", "呆滞库存诊断"),
    EXPIRY("EXPIRY", "效期库存诊断"),
    COST("COST", "库存成本诊断"),
    SAFETY("SAFETY", "安全库存诊断"),
    ABC_CLASSIFICATION("ABC_CLASSIFICATION", "ABC分类诊断");

    private final String code;
    private final String desc;

    DiagnosisType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
