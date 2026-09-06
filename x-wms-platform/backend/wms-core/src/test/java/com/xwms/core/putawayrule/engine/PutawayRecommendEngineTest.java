package com.xwms.core.putawayrule.engine;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xwms.core.putawayrule.cache.PutawayRuleCacheService;
import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.dto.PutawayRecommendResult;
import com.xwms.core.putawayrule.entity.PutawayRule;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;
import com.xwms.core.putawayrule.scorer.LocationScorer;
import com.xwms.core.putawayrule.scorer.LocationScorer.ScoredLocation;
import com.xwms.core.putawayrule.service.LocationQueryService;
import com.xwms.core.putawayrule.service.LocationQueryService.PutawayLocation;
import com.xwms.core.putawayrule.service.PutawayRuleService;
import com.xwms.core.putawayrule.strategy.RuleCodeStrategy;
import com.xwms.core.putawayrule.strategy.RuleCodeStrategyFactory;
import com.xwms.core.putawayrule.validator.ExtendedConstraintValidator;
import com.xwms.core.putawayrule.validator.LocationLimitValidator;
import com.xwms.core.putawayrule.validator.LocationValidationResult;
import com.xwms.core.putawayrule.validator.SpaceLimitValidator;
import com.xwms.core.support.BaseTest;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/** 库位推荐引擎单元测试 核心测试：规则解析(多级缓存) → 规则链执行 → 三维度校验 → 排序打分 → 结果封装 + 异常降级到TEMP 按 PutawayRecommendEngine 真实 API 重写（无 batchRecommend、recommend 走 try/catch fallback） */
class PutawayRecommendEngineTest extends BaseTest {

    @Mock private PutawayRuleService ruleService;
    @Mock private PutawayRuleCacheService ruleCacheService;
    @Mock private RuleCodeStrategyFactory strategyFactory;
    @Mock private LocationLimitValidator locationLimitValidator;
    @Mock private SpaceLimitValidator spaceLimitValidator;
    @Mock private ExtendedConstraintValidator extendedConstraintValidator;
    @Mock private LocationScorer locationScorer;
    @Mock private LocationQueryService locationQueryService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private PutawayRecommendEngine recommendEngine;

    private static final String WAREHOUSE = "WH01";
    private static final String OWNER = "OWNER01";
    private static final String SKU = "SKU001";
    private static final String RULE_CODE = "03";
    private static final String CANDIDATE_LOC = "LOC001";

    private PutawayRecommendContext buildContext() {
        PutawayRecommendContext context = new PutawayRecommendContext();
        context.setWarehouseCode(WAREHOUSE);
        context.setOwnerCode(OWNER);
        context.setSkuCode(SKU);
        return context;
    }

    private PutawayRule buildActiveRule() {
        PutawayRule rule = new PutawayRule();
        rule.setId(1L);
        rule.setRuleCode(RULE_CODE);
        rule.setRuleName("测试规则");
        rule.setStatus("ACTIVE");
        rule.setPriority(1);
        return rule;
    }

    private PutawayRuleLine buildLine(int lineNo) {
        PutawayRuleLine line = new PutawayRuleLine();
        line.setId((long) lineNo);
        line.setRuleId(1L);
        line.setLineNo(lineNo);
        line.setRuleCode(RULE_CODE);
        // conditionJson 为 null → matchCondition 直接返回 true
        // locationLimitsJson/spaceLimitsJson/extendedConstraintsJson 为 null → parseJsonList 返回空列表
        return line;
    }

    private PutawayLocation buildPutawayLocation(String code) {
        PutawayLocation loc = new PutawayLocation();
        loc.setLocationCode(code);
        loc.setAreaCode("A");
        loc.setLocationType("STORAGE");
        loc.setCapacity(new BigDecimal("1000"));
        loc.setUsedCapacity(BigDecimal.ZERO);
        loc.setMaxWeight(new BigDecimal("500"));
        loc.setCoordX(1);
        loc.setCoordY(1);
        loc.setCoordZ(1);
        return loc;
    }

    private ScoredLocation buildScoredLocation(PutawayLocation loc) {
        ScoredLocation scored = new ScoredLocation();
        scored.setLocation(loc);
        scored.setDistance(5.0);
        scored.setScore(80.0);
        return scored;
    }

    /** stub 三维度校验器全部通过（mock 默认返回 null 会导致 isPassed() NPE，必须显式 stub） */
    private void stubValidatorsPass() {
        when(locationLimitValidator.validate(any(), any(), any()))
                .thenReturn(LocationValidationResult.pass());
        when(spaceLimitValidator.validate(any(), any(), any()))
                .thenReturn(LocationValidationResult.pass());
        when(extendedConstraintValidator.validate(any(), any(), any()))
                .thenReturn(LocationValidationResult.pass());
    }

    /** stub 正常推荐成功路径的全部依赖 */
    private void stubHappyPath() {
        PutawayRule rule = buildActiveRule();
        PutawayRuleLine line = buildLine(1);

        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenReturn(List.of(1L));
        when(ruleCacheService.getRuleWithLines(1L)).thenReturn(rule);
        when(ruleService.getRuleLines(1L)).thenReturn(List.of(line));

        RuleCodeStrategy strategy = mock(RuleCodeStrategy.class);
        when(strategyFactory.getStrategy(RULE_CODE)).thenReturn(strategy);
        when(strategy.getCandidateLocations(any(), any())).thenReturn(List.of(CANDIDATE_LOC));

        PutawayLocation loc = buildPutawayLocation(CANDIDATE_LOC);
        when(locationQueryService.queryLocationsByArea(any(), any())).thenReturn(List.of(loc));
        when(locationQueryService.getReceivingPoint(WAREHOUSE)).thenReturn(buildPutawayLocation("RECV"));

        stubValidatorsPass();
        when(locationScorer.scoreAndSort(any(), any(), any()))
                .thenReturn(List.of(buildScoredLocation(loc)));
    }

    @Test
    @DisplayName("推荐引擎 - 正常推荐成功，返回候选库位列表")
    void testRecommend_Success() {
        PutawayRecommendContext context = buildContext();
        stubHappyPath();

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getCandidates());
        assertEquals(1, result.getCandidates().size());
        assertEquals(CANDIDATE_LOC, result.getCandidates().get(0).getLocationCode());
        assertEquals(RULE_CODE, result.getRuleCode());
        assertNotNull(result.getReason());
        assertTrue(result.getCostTime() >= 0);
    }

    @Test
    @DisplayName("推荐引擎 - 无匹配规则ID时返回失败")
    void testRecommend_NoMatchedRule() {
        PutawayRecommendContext context = buildContext();
        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("NO_RULE", result.getErrorCode());
        assertTrue(result.getErrorMessage().contains("未找到适用的上架规则"));
    }

    @Test
    @DisplayName("推荐引擎 - 匹配规则均非ACTIVE时返回失败")
    void testRecommend_InactiveRule() {
        PutawayRecommendContext context = buildContext();
        PutawayRule rule = buildActiveRule();
        rule.setStatus("INACTIVE");

        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenReturn(List.of(1L));
        when(ruleCacheService.getRuleWithLines(1L)).thenReturn(rule);

        PutawayRecommendResult result = recommendEngine.recommend(context);

        // resolveRule 过滤掉非 ACTIVE 规则后 rules 为空 → null → NO_RULE
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("NO_RULE", result.getErrorCode());
    }

    @Test
    @DisplayName("推荐引擎 - 规则无配置行时返回失败")
    void testRecommend_EmptyRuleLines() {
        PutawayRecommendContext context = buildContext();
        PutawayRule rule = buildActiveRule();

        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenReturn(List.of(1L));
        when(ruleCacheService.getRuleWithLines(1L)).thenReturn(rule);
        when(ruleService.getRuleLines(1L)).thenReturn(Collections.emptyList());

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("NO_RULE_LINE", result.getErrorCode());
    }

    @Test
    @DisplayName("推荐引擎 - 规则链执行无候选库位时返回失败")
    void testRecommend_NoCandidate() {
        PutawayRecommendContext context = buildContext();
        PutawayRule rule = buildActiveRule();
        PutawayRuleLine line = buildLine(1);

        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenReturn(List.of(1L));
        when(ruleCacheService.getRuleWithLines(1L)).thenReturn(rule);
        when(ruleService.getRuleLines(1L)).thenReturn(List.of(line));

        RuleCodeStrategy strategy = mock(RuleCodeStrategy.class);
        when(strategyFactory.getStrategy(RULE_CODE)).thenReturn(strategy);
        when(strategy.getCandidateLocations(any(), any())).thenReturn(Collections.emptyList());

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("NO_CANDIDATE", result.getErrorCode());
    }

    @Test
    @DisplayName("推荐引擎 - 所有候选库位校验不通过时返回失败")
    void testRecommend_AllCandidatesRejected() {
        PutawayRecommendContext context = buildContext();
        PutawayRule rule = buildActiveRule();
        PutawayRuleLine line = buildLine(1);

        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenReturn(List.of(1L));
        when(ruleCacheService.getRuleWithLines(1L)).thenReturn(rule);
        when(ruleService.getRuleLines(1L)).thenReturn(List.of(line));

        RuleCodeStrategy strategy = mock(RuleCodeStrategy.class);
        when(strategyFactory.getStrategy(RULE_CODE)).thenReturn(strategy);
        when(strategy.getCandidateLocations(any(), any())).thenReturn(List.of(CANDIDATE_LOC));

        PutawayLocation loc = buildPutawayLocation(CANDIDATE_LOC);
        when(locationQueryService.queryLocationsByArea(any(), any())).thenReturn(List.of(loc));

        // 第一维度校验失败 → 库位被拒
        when(locationLimitValidator.validate(any(), any(), any()))
                .thenReturn(LocationValidationResult.fail("LOCATION_LIMIT", "FAIL", "校验不通过"));

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("ALL_VALIDATE_FAIL", result.getErrorCode());
    }

    @Test
    @DisplayName("推荐引擎 - 上下文为空时抛NPE（catch块内日志再次解引用）")
    void testRecommend_NullContext_ThrowsNpe() {
        // recommend(null) → try 块内 context.getWarehouseCode() NPE → catch 块内 log.error 又对 context 解引用再次 NPE
        // 主代码对 null context 不降级，直接抛 NPE（非本任务范围改主代码）
        assertThrows(NullPointerException.class, () -> recommendEngine.recommend(null));
    }

    @Test
    @DisplayName("推荐引擎 - 仓库编码为空且无匹配规则时返回NO_RULE失败")
    void testRecommend_NullWarehouse_NoRule() {
        PutawayRecommendContext context = buildContext();
        context.setWarehouseCode(null);
        // 未 stub getMatchedRuleIds，Mockito 默认返回 null → resolveRule 返回 null → NO_RULE
        when(ruleCacheService.getMatchedRuleIds(any(), any(), any())).thenReturn(null);

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("NO_RULE", result.getErrorCode());
    }

    @Test
    @DisplayName("推荐引擎 - 规则链异常时降级到TEMP库位")
    void testRecommend_Exception_Fallback() {
        PutawayRecommendContext context = buildContext();
        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenThrow(new RuntimeException("缓存故障"));

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("FALLBACK", result.getRuleCode());
        assertEquals("TEMP", result.getCandidates().get(0).getLocationCode());
        assertTrue(result.getReason().contains("缓存故障"));
    }

    @Test
    @DisplayName("推荐引擎 - 记录推荐耗时")
    void testRecommend_RecordCostTime() {
        PutawayRecommendContext context = buildContext();
        stubHappyPath();

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertTrue(result.isSuccess());
        assertTrue(result.getCostTime() >= 0, "推荐耗时应大于等于0");
    }

    @Test
    @DisplayName("推荐引擎 - 多优先级规则取最高优先级")
    void testRecommend_MultipleRules_HighestPriority() {
        PutawayRecommendContext context = buildContext();

        PutawayRule lowPriority = buildActiveRule();
        lowPriority.setId(2L);
        lowPriority.setPriority(10);

        PutawayRule highPriority = buildActiveRule();
        highPriority.setId(1L);
        highPriority.setPriority(1);

        // getMatchedRuleIds 返回两个规则ID
        when(ruleCacheService.getMatchedRuleIds(any(), any(), any()))
                .thenReturn(List.of(2L, 1L));
        when(ruleCacheService.getRuleWithLines(2L)).thenReturn(lowPriority);
        when(ruleCacheService.getRuleWithLines(1L)).thenReturn(highPriority);
        when(ruleService.getRuleLines(1L)).thenReturn(List.of(buildLine(1)));

        RuleCodeStrategy strategy = mock(RuleCodeStrategy.class);
        when(strategyFactory.getStrategy(RULE_CODE)).thenReturn(strategy);
        when(strategy.getCandidateLocations(any(), any())).thenReturn(List.of(CANDIDATE_LOC));

        PutawayLocation loc = buildPutawayLocation(CANDIDATE_LOC);
        when(locationQueryService.queryLocationsByArea(any(), any())).thenReturn(List.of(loc));
        when(locationQueryService.getReceivingPoint(WAREHOUSE)).thenReturn(buildPutawayLocation("RECV"));
        stubValidatorsPass();
        when(locationScorer.scoreAndSort(any(), any(), any()))
                .thenReturn(List.of(buildScoredLocation(loc)));

        PutawayRecommendResult result = recommendEngine.recommend(context);

        assertTrue(result.isSuccess());
        // priority=1 的规则（id=1）应被选中
        assertEquals(1L, result.getRuleId());
        verify(ruleService).getRuleLines(1L);
    }
}
