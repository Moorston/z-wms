package com.xwms.core.putawayrule.validator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.xwms.core.putawayrule.dto.LocationLimit;
import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.service.LocationQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 库位限制校验器（第一维度） 7种限制类型： - EMPTY_BIN: 必须是空库位 - NO_MIX_SKU: 不许混产品（库位已有其他产品则不通过） - NO_MIX_LOT:
 * 不许混批号（库位已有其他批号则不通过） - SAME_SKU: 必须有相同产品（库位为空或有相同产品才通过） - SAME_LOT: 必须有相同批号（库位为空或有相同批号才通过） -
 * SAME_PRODUCT_GROUP: 必须有相同产品组 - DEEP_LANE_FIFO: 多伸位生产日期排序（新入库生产日期≥库位最大生产日期）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationLimitValidator {

    private static final String DIMENSION = "LOCATION_LIMIT";

    /** 校验库位是否满足所有库位限制 */
    public LocationValidationResult validate(
            List<LocationLimit> limits,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (limits == null || limits.isEmpty()) {
            return LocationValidationResult.pass();
        }

        for (LocationLimit limit : limits) {
            LocationValidationResult result = validateSingle(limit, location, context);
            if (!result.isPassed()) {
                return result;
            }
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateSingle(
            LocationLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        String type = limit.getType();
        if (type == null) {
            return LocationValidationResult.pass();
        }

        switch (type.toUpperCase()) {
            case "EMPTY_BIN":
                return validateEmptyBin(location);
            case "NO_MIX_SKU":
                return validateNoMixSku(location, context);
            case "NO_MIX_LOT":
                return validateNoMixLot(location, context);
            case "SAME_SKU":
                return validateSameSku(location, context);
            case "SAME_LOT":
                return validateSameLot(location, context);
            case "SAME_PRODUCT_GROUP":
                return validateSameProductGroup(location, limit, context);
            case "DEEP_LANE_FIFO":
                return validateDeepLaneFifo(location, context);
            default:
                log.warn("未知的库位限制类型: {}", type);
                return LocationValidationResult.pass();
        }
    }

    private LocationValidationResult validateEmptyBin(
            LocationQueryService.PutawayLocation location) {
        if (location.getCurrentQty() != null
                && location.getCurrentQty().compareTo(java.math.BigDecimal.ZERO) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION, "NOT_EMPTY", "库位非空，当前库存: " + location.getCurrentQty());
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateNoMixSku(
            LocationQueryService.PutawayLocation location, PutawayRecommendContext context) {
        String currentSku = location.getCurrentSku();
        if (currentSku != null
                && !currentSku.isEmpty()
                && !currentSku.equals(context.getSkuCode())) {
            return LocationValidationResult.fail(
                    DIMENSION, "SKU_MIXED", "库位已有其他产品: " + currentSku + "，不允许混放");
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateNoMixLot(
            LocationQueryService.PutawayLocation location, PutawayRecommendContext context) {
        String currentBatch = location.getCurrentBatch();
        if (currentBatch != null
                && !currentBatch.isEmpty()
                && !currentBatch.equals(context.getLot())) {
            return LocationValidationResult.fail(
                    DIMENSION, "LOT_MIXED", "库位已有其他批号: " + currentBatch + "，不允许混放");
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateSameSku(
            LocationQueryService.PutawayLocation location, PutawayRecommendContext context) {
        String currentSku = location.getCurrentSku();
        if (currentSku != null
                && !currentSku.isEmpty()
                && !currentSku.equals(context.getSkuCode())) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "DIFFERENT_SKU",
                    "库位产品: " + currentSku + "，与上架产品: " + context.getSkuCode() + " 不一致");
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateSameLot(
            LocationQueryService.PutawayLocation location, PutawayRecommendContext context) {
        String currentBatch = location.getCurrentBatch();
        if (currentBatch != null
                && !currentBatch.isEmpty()
                && !currentBatch.equals(context.getLot())) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "DIFFERENT_LOT",
                    "库位批号: " + currentBatch + "，与上架批号: " + context.getLot() + " 不一致");
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateSameProductGroup(
            LocationQueryService.PutawayLocation location,
            LocationLimit limit,
            PutawayRecommendContext context) {
        // 实际项目中需从库位属性或产品档案获取产品组
        // 简化实现：校验库位当前产品组与上架产品组是否一致
        String requiredGroup = limit.getValue();
        if (requiredGroup != null
                && context.getProductGroup() != null
                && !requiredGroup.equals(context.getProductGroup())) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "DIFFERENT_PRODUCT_GROUP",
                    "上架产品组: " + context.getProductGroup() + "，要求产品组: " + requiredGroup);
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateDeepLaneFifo(
            LocationQueryService.PutawayLocation location, PutawayRecommendContext context) {
        // 多伸位货架：新入库生产日期必须≥库位已有最大生产日期
        if (location.getExpiryDate() != null && context.getProductionDate() != null) {
            if (context.getProductionDate().isBefore(location.getExpiryDate())) {
                return LocationValidationResult.fail(
                        DIMENSION,
                        "FIFO_VIOLATION",
                        "新入库生产日期: "
                                + context.getProductionDate()
                                + " 早于库位已有生产日期: "
                                + location.getExpiryDate());
            }
        }
        return LocationValidationResult.pass();
    }
}
