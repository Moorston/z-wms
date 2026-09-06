package com.xwms.core.performance.enums;

import lombok.Getter;

/** 绩效等级 */
@Getter
public enum PerformanceGrade {
    S("S", "卓越", 90, 100),
    A("A", "优秀", 80, 89),
    B("B", "良好", 70, 79),
    C("C", "合格", 60, 69),
    D("D", "不合格", 0, 59);

    private final String code;
    private final String desc;
    private final int minScore;
    private final int maxScore;

    PerformanceGrade(String code, String desc, int minScore, int maxScore) {
        this.code = code;
        this.desc = desc;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public static PerformanceGrade fromScore(double score) {
        for (PerformanceGrade grade : values()) {
            if (score >= grade.minScore && score <= grade.maxScore) {
                return grade;
            }
        }
        return D;
    }
}
