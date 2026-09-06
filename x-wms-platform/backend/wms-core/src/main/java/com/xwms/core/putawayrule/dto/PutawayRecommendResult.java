package com.xwms.core.putawayrule.dto;

import java.util.List;

import lombok.Data;

/** 上架推荐结果 */
@Data
public class PutawayRecommendResult {

    /** 是否成功 */
    private boolean success;

    /** 命中的规则ID */
    private Long ruleId;

    /** 命中的规则编码 */
    private String ruleCode;

    /** 命中的规则行号 */
    private Integer hitLineNo;

    /** 推荐库位列表（按优先级排序） */
    private List<RecommendLocation> candidates;

    /** 推荐理由 */
    private String reason;

    /** 错误码（失败时） */
    private String errorCode;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 推荐耗时(ms) */
    private long costTime;

    @Data
    public static class RecommendLocation {
        /** 库位编码 */
        private String locationCode;

        /** 库区编码 */
        private String zoneCode;

        /** 距离基点(m) */
        private double distance;

        /** 可用体积利用率 */
        private double availableVolumeRatio;

        /** 可用重量 */
        private double availableWeight;

        /** 综合分数 */
        private double score;

        /** 库位类型 */
        private String locationType;

        /** 当前库存SKU */
        private String currentSku;

        /** 当前库存批号 */
        private String currentLot;
    }

    public static PutawayRecommendResult success(
            Long ruleId,
            String ruleCode,
            Integer hitLineNo,
            List<RecommendLocation> candidates,
            String reason) {
        PutawayRecommendResult result = new PutawayRecommendResult();
        result.setSuccess(true);
        result.setRuleId(ruleId);
        result.setRuleCode(ruleCode);
        result.setHitLineNo(hitLineNo);
        result.setCandidates(candidates);
        result.setReason(reason);
        return result;
    }

    public static PutawayRecommendResult fail(String errorCode, String errorMessage) {
        PutawayRecommendResult result = new PutawayRecommendResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
