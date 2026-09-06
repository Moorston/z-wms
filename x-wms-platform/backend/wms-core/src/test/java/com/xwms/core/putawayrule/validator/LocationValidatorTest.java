package com.xwms.core.putawayrule.validator;

import static org.junit.jupiter.api.Assertions.*;

import com.xwms.core.putawayrule.dto.ExtendedConstraint;
import com.xwms.core.putawayrule.dto.LocationLimit;
import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.dto.SpaceLimit;
import com.xwms.core.putawayrule.service.LocationQueryService;
import com.xwms.core.putawayrule.service.LocationQueryService.PutawayLocation;
import com.xwms.core.support.BaseTest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 三维度校验器单元测试 覆盖：库位限制校验（7种）/空间限制校验（6种）/扩展约束校验（5种） 均走 validate(List, PutawayLocation, PutawayRecommendContext) 三参签名 */
class LocationValidatorTest extends BaseTest {

    private LocationLimitValidator locationLimitValidator;
    private SpaceLimitValidator spaceLimitValidator;
    private ExtendedConstraintValidator extendedConstraintValidator;

    @BeforeEach
    public void setUpValidators() {
        locationLimitValidator = new LocationLimitValidator();
        spaceLimitValidator = new SpaceLimitValidator();
        extendedConstraintValidator = new ExtendedConstraintValidator();
    }

    private PutawayLocation buildLocation() {
        PutawayLocation loc = new PutawayLocation();
        loc.setLocationCode("LOC001");
        loc.setLocationType("STORAGE");
        loc.setAreaCode("A");
        loc.setCapacity(new BigDecimal("1000"));
        loc.setUsedCapacity(BigDecimal.ZERO);
        loc.setMaxWeight(new BigDecimal("500"));
        return loc;
    }

    private PutawayRecommendContext buildContext() {
        PutawayRecommendContext ctx = new PutawayRecommendContext();
        ctx.setSkuCode("SKU001");
        ctx.setLot("BATCH001");
        ctx.setProductGroup("PG01");
        ctx.setQuantity(BigDecimal.TEN);
        return ctx;
    }

    // ============================================================
    // 库位限制校验测试（LocationLimitValidator，7 种 type）
    // ============================================================

    @Test
    @DisplayName("库位限制 - 空库位限制：空库位通过")
    void testLocationLimit_EmptyBin_Pass() {
        LocationLimit limit = new LocationLimit();
        limit.setType("EMPTY_BIN");
        PutawayLocation loc = buildLocation(); // currentQty=null 视为空
        assertTrue(locationLimitValidator.validate(List.of(limit), loc, buildContext()).isPassed());
    }

    @Test
    @DisplayName("库位限制 - 空库位限制：非空库位拒绝")
    void testLocationLimit_EmptyBin_Reject() {
        LocationLimit limit = new LocationLimit();
        limit.setType("EMPTY_BIN");
        PutawayLocation loc = buildLocation();
        loc.setCurrentQty(BigDecimal.TEN);
        LocationValidationResult result =
                locationLimitValidator.validate(List.of(limit), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("NOT_EMPTY", result.getFailCode());
        assertTrue(result.getFailMessage().contains("非空"));
    }

    @Test
    @DisplayName("库位限制 - 不许混产品：相同产品通过")
    void testLocationLimit_NoMixSku_SameSku_Pass() {
        LocationLimit limit = new LocationLimit();
        limit.setType("NO_MIX_SKU");
        PutawayLocation loc = buildLocation();
        loc.setCurrentSku("SKU001"); // 与 context.skuCode 相同
        assertTrue(locationLimitValidator.validate(List.of(limit), loc, buildContext()).isPassed());
    }

    @Test
    @DisplayName("库位限制 - 不许混产品：不同产品拒绝")
    void testLocationLimit_NoMixSku_DiffSku_Reject() {
        LocationLimit limit = new LocationLimit();
        limit.setType("NO_MIX_SKU");
        PutawayLocation loc = buildLocation();
        loc.setCurrentSku("SKU002"); // 与 context.skuCode 不同
        LocationValidationResult result =
                locationLimitValidator.validate(List.of(limit), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("SKU_MIXED", result.getFailCode());
    }

    @Test
    @DisplayName("库位限制 - 不许混批号：相同批号通过")
    void testLocationLimit_NoMixLot_SameLot_Pass() {
        LocationLimit limit = new LocationLimit();
        limit.setType("NO_MIX_LOT");
        PutawayLocation loc = buildLocation();
        loc.setCurrentBatch("BATCH001"); // 与 context.lot 相同
        assertTrue(locationLimitValidator.validate(List.of(limit), loc, buildContext()).isPassed());
    }

    @Test
    @DisplayName("库位限制 - 不许混批号：不同批号拒绝")
    void testLocationLimit_NoMixLot_DiffLot_Reject() {
        LocationLimit limit = new LocationLimit();
        limit.setType("NO_MIX_LOT");
        PutawayLocation loc = buildLocation();
        loc.setCurrentBatch("BATCH002"); // 与 context.lot 不同
        LocationValidationResult result =
                locationLimitValidator.validate(List.of(limit), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("LOT_MIXED", result.getFailCode());
    }

    @Test
    @DisplayName("库位限制 - 必须有相同产品：相同通过，不同拒绝")
    void testLocationLimit_SameSku() {
        LocationLimit limit = new LocationLimit();
        limit.setType("SAME_SKU");
        PutawayLocation loc = buildLocation();
        loc.setCurrentSku("SKU001");
        assertTrue(locationLimitValidator.validate(List.of(limit), loc, buildContext()).isPassed());

        loc.setCurrentSku("SKU999");
        LocationValidationResult result =
                locationLimitValidator.validate(List.of(limit), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("DIFFERENT_SKU", result.getFailCode());
    }

    @Test
    @DisplayName("库位限制 - 相同产品组：匹配通过，不匹配拒绝")
    void testLocationLimit_SameProductGroup() {
        LocationLimit limit = new LocationLimit();
        limit.setType("SAME_PRODUCT_GROUP");
        limit.setValue("PG01");
        PutawayLocation loc = buildLocation();
        assertTrue(locationLimitValidator.validate(List.of(limit), loc, buildContext()).isPassed());

        limit.setValue("PG99");
        LocationValidationResult result =
                locationLimitValidator.validate(List.of(limit), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("DIFFERENT_PRODUCT_GROUP", result.getFailCode());
    }

    @Test
    @DisplayName("库位限制 - 未知类型默认通过")
    void testLocationLimit_UnknownType_Pass() {
        LocationLimit limit = new LocationLimit();
        limit.setType("UNKNOWN_TYPE");
        assertTrue(locationLimitValidator.validate(List.of(limit), buildLocation(), buildContext()).isPassed());
    }

    @Test
    @DisplayName("库位限制 - 空 limits 列表通过")
    void testLocationLimit_EmptyList_Pass() {
        assertTrue(
                locationLimitValidator
                        .validate(Collections.emptyList(), buildLocation(), buildContext())
                        .isPassed());
    }

    @Test
    @DisplayName("库位限制 - null limits 列表通过")
    void testLocationLimit_NullList_Pass() {
        assertTrue(locationLimitValidator.validate(null, buildLocation(), buildContext()).isPassed());
    }

    // ============================================================
    // 空间限制校验测试（SpaceLimitValidator，6 种 type）
    // ============================================================

    @Test
    @DisplayName("空间限制 - 体积限制：所需体积未超阈值且库位剩余足够则通过")
    void testSpaceLimit_Volume_Pass() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("VOLUME");
        limit.setThreshold(new BigDecimal("5000"));
        PutawayLocation loc = buildLocation();
        loc.setCapacity(new BigDecimal("1000"));
        loc.setUsedCapacity(new BigDecimal("200"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setProductVolume(new BigDecimal("10")); // 10 * 10 = 100 ≤ 5000 且 ≤ 800
        assertTrue(spaceLimitValidator.validate(List.of(limit), loc, ctx).isPassed());
    }

    @Test
    @DisplayName("空间限制 - 体积限制：所需体积超阈值拒绝")
    void testSpaceLimit_Volume_ExceedThreshold_Reject() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("VOLUME");
        limit.setThreshold(new BigDecimal("50"));
        PutawayLocation loc = buildLocation();
        PutawayRecommendContext ctx = buildContext();
        ctx.setProductVolume(new BigDecimal("10")); // 10 * 10 = 100 > 50
        LocationValidationResult result = spaceLimitValidator.validate(List.of(limit), loc, ctx);
        assertFalse(result.isPassed());
        assertEquals("VOLUME_EXCEED_THRESHOLD", result.getFailCode());
    }

    @Test
    @DisplayName("空间限制 - 体积限制：库位剩余不足拒绝")
    void testSpaceLimit_Volume_Insufficient_Reject() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("VOLUME");
        limit.setThreshold(new BigDecimal("99999")); // 阈值放高，聚焦库位剩余不足
        PutawayLocation loc = buildLocation();
        loc.setCapacity(new BigDecimal("100"));
        loc.setUsedCapacity(new BigDecimal("95")); // 剩余 5
        PutawayRecommendContext ctx = buildContext();
        ctx.setProductVolume(new BigDecimal("10")); // 10 * 10 = 100 > 5
        LocationValidationResult result = spaceLimitValidator.validate(List.of(limit), loc, ctx);
        assertFalse(result.isPassed());
        assertEquals("VOLUME_INSUFFICIENT", result.getFailCode());
    }

    @Test
    @DisplayName("空间限制 - 重量限制：所需重量未超承重通过")
    void testSpaceLimit_Weight_Pass() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("WEIGHT");
        PutawayLocation loc = buildLocation();
        loc.setMaxWeight(new BigDecimal("500"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setProductWeight(new BigDecimal("10")); // 10 * 10 = 100 ≤ 500
        assertTrue(spaceLimitValidator.validate(List.of(limit), loc, ctx).isPassed());
    }

    @Test
    @DisplayName("空间限制 - 重量限制：所需重量超承重拒绝")
    void testSpaceLimit_Weight_Exceed_Reject() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("WEIGHT");
        PutawayLocation loc = buildLocation();
        loc.setMaxWeight(new BigDecimal("50"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setProductWeight(new BigDecimal("10")); // 10 * 10 = 100 > 50
        LocationValidationResult result = spaceLimitValidator.validate(List.of(limit), loc, ctx);
        assertFalse(result.isPassed());
        assertEquals("WEIGHT_INSUFFICIENT", result.getFailCode());
    }

    @Test
    @DisplayName("空间限制 - 数量限制：未超阈值通过")
    void testSpaceLimit_Quantity_Pass() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("QUANTITY");
        limit.setThreshold(new BigDecimal("100"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setQuantity(BigDecimal.TEN); // 10 ≤ 100
        assertTrue(spaceLimitValidator.validate(List.of(limit), buildLocation(), ctx).isPassed());
    }

    @Test
    @DisplayName("空间限制 - 数量限制：超阈值拒绝")
    void testSpaceLimit_Quantity_Exceed_Reject() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("QUANTITY");
        limit.setThreshold(new BigDecimal("5"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setQuantity(BigDecimal.TEN); // 10 > 5
        LocationValidationResult result =
                spaceLimitValidator.validate(List.of(limit), buildLocation(), ctx);
        assertFalse(result.isPassed());
        assertEquals("QUANTITY_EXCEED", result.getFailCode());
    }

    @Test
    @DisplayName("空间限制 - 托盘数限制：未超通过")
    void testSpaceLimit_Pallet_Pass() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("PALLET");
        limit.setThreshold(new BigDecimal("2"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setPalletCount(1); // 1 ≤ 2
        assertTrue(spaceLimitValidator.validate(List.of(limit), buildLocation(), ctx).isPassed());
    }

    @Test
    @DisplayName("空间限制 - 托盘数限制：超阈值拒绝")
    void testSpaceLimit_Pallet_Exceed_Reject() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("PALLET");
        limit.setThreshold(new BigDecimal("1"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setPalletCount(3); // 3 > 1
        LocationValidationResult result =
                spaceLimitValidator.validate(List.of(limit), buildLocation(), ctx);
        assertFalse(result.isPassed());
        assertEquals("PALLET_EXCEED", result.getFailCode());
    }

    @Test
    @DisplayName("空间限制 - 箱数限制：超阈值拒绝")
    void testSpaceLimit_Case_Exceed_Reject() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("CASE");
        limit.setThreshold(new BigDecimal("5"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setCaseCount(10); // 10 > 5
        LocationValidationResult result =
                spaceLimitValidator.validate(List.of(limit), buildLocation(), ctx);
        assertFalse(result.isPassed());
        assertEquals("CASE_EXCEED", result.getFailCode());
    }

    @Test
    @DisplayName("空间限制 - 尺寸限制：产品长超库位长拒绝")
    void testSpaceLimit_Dimension_LengthExceed_Reject() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("DIMENSION");
        limit.setMaxLength(new BigDecimal("500"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setProductLength(new BigDecimal("600")); // 600 > 500
        LocationValidationResult result =
                spaceLimitValidator.validate(List.of(limit), buildLocation(), ctx);
        assertFalse(result.isPassed());
        assertEquals("LENGTH_EXCEED", result.getFailCode());
    }

    @Test
    @DisplayName("空间限制 - 尺寸限制：长宽高均未超通过")
    void testSpaceLimit_Dimension_Pass() {
        SpaceLimit limit = new SpaceLimit();
        limit.setType("DIMENSION");
        limit.setMaxLength(new BigDecimal("500"));
        limit.setMaxWidth(new BigDecimal("300"));
        limit.setMaxHeight(new BigDecimal("200"));
        PutawayRecommendContext ctx = buildContext();
        ctx.setProductLength(new BigDecimal("400"));
        ctx.setProductWidth(new BigDecimal("200"));
        ctx.setProductHeight(new BigDecimal("100"));
        assertTrue(spaceLimitValidator.validate(List.of(limit), buildLocation(), ctx).isPassed());
    }

    @Test
    @DisplayName("空间限制 - 空 limits 列表通过")
    void testSpaceLimit_EmptyList_Pass() {
        assertTrue(
                spaceLimitValidator
                        .validate(Collections.emptyList(), buildLocation(), buildContext())
                        .isPassed());
    }

    // ============================================================
    // 扩展约束校验测试（ExtendedConstraintValidator，5 种 type）
    // ============================================================

    @Test
    @DisplayName("扩展约束 - 库位类型匹配通过")
    void testExtendedConstraint_LocationType_Match_Pass() {
        ExtendedConstraint constraint = new ExtendedConstraint();
        constraint.setType("LOCATION_TYPE");
        constraint.setValue("STORAGE");
        PutawayLocation loc = buildLocation();
        loc.setLocationType("STORAGE");
        assertTrue(
                extendedConstraintValidator
                        .validate(List.of(constraint), loc, buildContext())
                        .isPassed());
    }

    @Test
    @DisplayName("扩展约束 - 库位类型不匹配拒绝")
    void testExtendedConstraint_LocationType_NoMatch_Reject() {
        ExtendedConstraint constraint = new ExtendedConstraint();
        constraint.setType("LOCATION_TYPE");
        constraint.setValue("STORAGE");
        PutawayLocation loc = buildLocation();
        loc.setLocationType("RECEIVE");
        LocationValidationResult result =
                extendedConstraintValidator.validate(List.of(constraint), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("LOCATION_TYPE_MISMATCH", result.getFailCode());
    }

    @Test
    @DisplayName("扩展约束 - 周转区匹配通过")
    void testExtendedConstraint_CycleZone_Match_Pass() {
        ExtendedConstraint constraint = new ExtendedConstraint();
        constraint.setType("CYCLE_ZONE");
        constraint.setValue("A");
        PutawayLocation loc = buildLocation();
        loc.setAreaCode("A-01"); // 以 A 开头
        assertTrue(
                extendedConstraintValidator
                        .validate(List.of(constraint), loc, buildContext())
                        .isPassed());
    }

    @Test
    @DisplayName("扩展约束 - 周转区不匹配拒绝")
    void testExtendedConstraint_CycleZone_NoMatch_Reject() {
        ExtendedConstraint constraint = new ExtendedConstraint();
        constraint.setType("CYCLE_ZONE");
        constraint.setValue("A");
        PutawayLocation loc = buildLocation();
        loc.setAreaCode("C-01"); // 不以 A 开头
        LocationValidationResult result =
                extendedConstraintValidator.validate(List.of(constraint), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("CYCLE_ZONE_MISMATCH", result.getFailCode());
    }

    @Test
    @DisplayName("扩展约束 - 库位属性不包含拒绝")
    void testExtendedConstraint_LocationAttr_NoMatch_Reject() {
        ExtendedConstraint constraint = new ExtendedConstraint();
        constraint.setType("LOCATION_ATTR");
        constraint.setValue("COLD");
        PutawayLocation loc = buildLocation();
        loc.setLocationAttrs("{\"temp\":\"NORMAL\"}"); // 不含 COLD
        LocationValidationResult result =
                extendedConstraintValidator.validate(List.of(constraint), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("LOCATION_ATTR_MISMATCH", result.getFailCode());
    }

    @Test
    @DisplayName("扩展约束 - 无约束时空列表通过")
    void testExtendedConstraint_EmptyList_Pass() {
        assertTrue(
                extendedConstraintValidator
                        .validate(Collections.emptyList(), buildLocation(), buildContext())
                        .isPassed());
    }

    @Test
    @DisplayName("扩展约束 - 多约束全部通过")
    void testExtendedConstraint_MultipleAllPass() {
        ExtendedConstraint c1 = new ExtendedConstraint();
        c1.setType("LOCATION_TYPE");
        c1.setValue("STORAGE");
        ExtendedConstraint c2 = new ExtendedConstraint();
        c2.setType("CYCLE_ZONE");
        c2.setValue("A");
        PutawayLocation loc = buildLocation();
        loc.setLocationType("STORAGE");
        loc.setAreaCode("A-01");
        assertTrue(
                extendedConstraintValidator.validate(Arrays.asList(c1, c2), loc, buildContext()).isPassed());
    }

    @Test
    @DisplayName("扩展约束 - 多约束首个失败即返回")
    void testExtendedConstraint_MultipleFirstFail() {
        ExtendedConstraint c1 = new ExtendedConstraint();
        c1.setType("LOCATION_TYPE");
        c1.setValue("STORAGE");
        ExtendedConstraint c2 = new ExtendedConstraint();
        c2.setType("CYCLE_ZONE");
        c2.setValue("A");
        PutawayLocation loc = buildLocation();
        loc.setLocationType("RECEIVE"); // 第一个就失败
        loc.setAreaCode("A-01");
        LocationValidationResult result =
                extendedConstraintValidator.validate(Arrays.asList(c1, c2), loc, buildContext());
        assertFalse(result.isPassed());
        assertEquals("LOCATION_TYPE_MISMATCH", result.getFailCode());
    }
}
