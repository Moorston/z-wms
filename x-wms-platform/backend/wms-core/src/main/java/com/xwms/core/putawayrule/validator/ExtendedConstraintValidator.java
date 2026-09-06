package com.xwms.core.putawayrule.validator;

import java.util.List;

import org.springframework.stereotype.Component;

import com.xwms.core.putawayrule.dto.ExtendedConstraint;
import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.service.LocationQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 扩展约束校验器（第三维度） 5种约束类型： - LOCATION_TYPE: 库位类型限制（STORAGE/PICK/RECEIVE等） - LOCATION_ATTR:
 * 库位属性限制（温层/危化品/贵重品等） - CYCLE_ZONE: 周转区限制（A/B/C类周转区） - LOCATION_GROUP: 库位组限制 - SKU_LOCATION_LIMIT:
 * 指定库区产品库位个数限制（防止单SKU占满快速周转区）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtendedConstraintValidator {

    private static final String DIMENSION = "EXTENDED_CONSTRAINT";

    /** 校验库位是否满足所有扩展约束 */
    public LocationValidationResult validate(
            List<ExtendedConstraint> constraints,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        if (constraints == null || constraints.isEmpty()) {
            return LocationValidationResult.pass();
        }

        for (ExtendedConstraint constraint : constraints) {
            LocationValidationResult result = validateSingle(constraint, location, context);
            if (!result.isPassed()) {
                return result;
            }
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateSingle(
            ExtendedConstraint constraint,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        String type = constraint.getType();
        if (type == null) {
            return LocationValidationResult.pass();
        }

        switch (type.toUpperCase()) {
            case "LOCATION_TYPE":
                return validateLocationType(constraint, location);
            case "LOCATION_ATTR":
                return validateLocationAttr(constraint, location);
            case "CYCLE_ZONE":
                return validateCycleZone(constraint, location);
            case "LOCATION_GROUP":
                return validateLocationGroup(constraint, location);
            case "SKU_LOCATION_LIMIT":
                return validateSkuLocationLimit(constraint, location, context);
            default:
                log.warn("未知的扩展约束类型: {}", type);
                return LocationValidationResult.pass();
        }
    }

    private LocationValidationResult validateLocationType(
            ExtendedConstraint constraint, LocationQueryService.PutawayLocation location) {
        String requiredType = constraint.getValue();
        if (requiredType != null
                && location.getLocationType() != null
                && !requiredType.equalsIgnoreCase(location.getLocationType())) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "LOCATION_TYPE_MISMATCH",
                    "库位类型: " + location.getLocationType() + "，要求: " + requiredType);
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateLocationAttr(
            ExtendedConstraint constraint, LocationQueryService.PutawayLocation location) {
        // 库位属性校验（温层/危化品/贵重品等）
        // 实际项目中从库位属性JSON中解析，这里简化实现
        String requiredAttr = constraint.getValue();
        if (requiredAttr != null
                && location.getLocationAttrs() != null
                && !location.getLocationAttrs().contains(requiredAttr)) {
            return LocationValidationResult.fail(
                    DIMENSION, "LOCATION_ATTR_MISMATCH", "库位不包含属性: " + requiredAttr);
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateCycleZone(
            ExtendedConstraint constraint, LocationQueryService.PutawayLocation location) {
        // 周转区校验：A类品只能进A类周转区
        String requiredZone = constraint.getValue();
        if (requiredZone != null
                && location.getAreaCode() != null
                && !location.getAreaCode().startsWith(requiredZone)) {
            return LocationValidationResult.fail(
                    DIMENSION,
                    "CYCLE_ZONE_MISMATCH",
                    "库位库区: " + location.getAreaCode() + "，要求周转区: " + requiredZone);
        }
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateLocationGroup(
            ExtendedConstraint constraint, LocationQueryService.PutawayLocation location) {
        // 库位组校验（实际项目中从库位主数据获取库位组）
        // 简化实现：通过库区编码前缀判断
        return LocationValidationResult.pass();
    }

    private LocationValidationResult validateSkuLocationLimit(
            ExtendedConstraint constraint,
            LocationQueryService.PutawayLocation location,
            PutawayRecommendContext context) {
        // 指定库区产品库位个数限制
        // 实际项目中需查询该SKU在指定库区已占用的库位数
        // 简化实现：如果库位已有该SKU库存，则通过（合并）；否则需查询计数
        Integer maxLocations = constraint.getMaxLocations();
        if (maxLocations != null
                && location.getCurrentSku() != null
                && location.getCurrentSku().equals(context.getSkuCode())) {
            // 库位已有该SKU，合并上架，不增加库位数
            return LocationValidationResult.pass();
        }
        // TODO: 查询该SKU在指定库区已占用库位数，判断是否超过maxLocations
        return LocationValidationResult.pass();
    }
}
