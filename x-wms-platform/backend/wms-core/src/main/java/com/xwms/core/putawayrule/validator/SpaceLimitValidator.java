package com.xwms.core.putawayrule.validator;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.dto.SpaceLimit;
import com.xwms.core.putawayrule.service.LocationQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 空间限制校验器（第二维度） 6种限制类型： - VOLUME: 体积限制（产品体积×数量 ≤ 库位剩余体积） - WEIGHT: 重量限制（产品重量×数量 ≤ 库位剩余承重） -
 * QUANTITY: 数量限制（上架数量 ≤ 库位数量限制） - PALLET: 托盘数限制（上架托盘数 ≤ 库位托盘限制） - CASE: 箱数限制（上架箱数 ≤ 库位箱数限制） -
 * DIMENSION: 尺寸限制（产品长宽高 ≤ 库位长宽高）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpaceLimitValidator {

    private static final String DIMENSION = "SPACE_LIMIT";

    /** 校验库位是否满足所有空间限制 */
    public LocationValidationResult validate(
            List<SpaceLimit> limits,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (limits == null || limits.isEmpty()) {
            return LocationValidationResult.pass();
        }

        for (SpaceLimit limit : limits) {
            LocationValidationResult result = validateSingle(limit, location, context);
            if (!result.isPassed()) {
                return result;
            }
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateSingle(
            SpaceLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        String type = limit.getType();
        if (type == null) {
            return LocationValidationResult.pass();
        }

        switch (type.toUpperCase()) {
            case "VOLUME":
                return validateVolume(limit, location, context);
            case "WEIGHT":
                return validateWeight(limit, location, context);
            case "QUANTITY":
                return validateQuantity(limit, location, context);
            case "PALLET":
                return validatePallet(limit, location, context);
            case "CASE":
                return validateCase(limit, location, context);
            case "DIMENSION":
                return validateDimension(limit, location, context);
            default:
                log.warn("未知的空间限制类型: {}", type);
                return LocationValidationResult.pass();
        }
    }

    private LocationValidationResult validateVolume(
            SpaceLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (context.getProductVolume() == null || context.getQuantity() == null) {
            return LocationValidationResult.pass();
        }
        BigDecimal requiredVolume = context.getProductVolume().multiply(context.getQuantity());
        BigDecimal availableVolume =
                location.getCapacity() != null && location.getUsedCapacity() != null
                        ? location.getCapacity().subtract(location.getUsedCapacity())
                        : BigDecimal.valueOf(Long.MAX_VALUE);

        if (limit.getThreshold() != null && requiredVolume.compareTo(limit.getThreshold()) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "VOLUME_EXCEED_THRESHOLD",
                    "所需体积: " + requiredVolume + " 超过规则阈值: " + limit.getThreshold());
        }
        if (requiredVolume.compareTo(availableVolume) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "VOLUME_INSUFFICIENT",
                    "所需体积: " + requiredVolume + "，库位剩余体积: " + availableVolume);
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateWeight(
            SpaceLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (context.getProductWeight() == null || context.getQuantity() == null) {
            return LocationValidationResult.pass();
        }
        BigDecimal requiredWeight = context.getProductWeight().multiply(context.getQuantity());
        BigDecimal availableWeight =
                location.getMaxWeight() != null
                        ? location.getMaxWeight()
                        : BigDecimal.valueOf(Long.MAX_VALUE);

        if (requiredWeight.compareTo(availableWeight) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "WEIGHT_INSUFFICIENT",
                    "所需重量: " + requiredWeight + "，库位最大承重: " + availableWeight);
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateQuantity(
            SpaceLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (context.getQuantity() == null || limit.getThreshold() == null) {
            return LocationValidationResult.pass();
        }
        if (context.getQuantity().compareTo(limit.getThreshold()) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "QUANTITY_EXCEED",
                    "上架数量: " + context.getQuantity() + " 超过限制: " + limit.getThreshold());
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validatePallet(
            SpaceLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (context.getPalletCount() == null || limit.getThreshold() == null) {
            return LocationValidationResult.pass();
        }
        if (context.getPalletCount() > limit.getThreshold().intValue()) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "PALLET_EXCEED",
                    "上架托盘数: " + context.getPalletCount() + " 超过限制: " + limit.getThreshold());
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateCase(
            SpaceLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (context.getCaseCount() == null || limit.getThreshold() == null) {
            return LocationValidationResult.pass();
        }
        if (context.getCaseCount() > limit.getThreshold().intValue()) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "CASE_EXCEED",
                    "上架箱数: " + context.getCaseCount() + " 超过限制: " + limit.getThreshold());
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateDimension(
            SpaceLimit limit,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        // 尺寸限制：产品长宽高必须≤库位长宽高
        // 实际项目中库位尺寸从库位主数据获取，这里简化校验规则配置的阈值
        if (limit.getMaxLength() != null
                && context.getProductLength() != null
                && context.getProductLength().compareTo(limit.getMaxLength()) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "LENGTH_EXCEED",
                    "产品长: " + context.getProductLength() + " 超过库位长: " + limit.getMaxLength());
        }
        if (limit.getMaxWidth() != null
                && context.getProductWidth() != null
                && context.getProductWidth().compareTo(limit.getMaxWidth()) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "WIDTH_EXCEED",
                    "产品宽: " + context.getProductWidth() + " 超过库位宽: " + limit.getMaxWidth());
        }
        if (limit.getMaxHeight() != null
                && context.getProductHeight() != null
                && context.getProductHeight().compareTo(limit.getMaxHeight()) > 0) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "HEIGHT_EXCEED",
                    "产品高: " + context.getProductHeight() + " 超过库位高: " + limit.getMaxHeight());
        }
        return LocationValidationResult.pass();
    }
}
