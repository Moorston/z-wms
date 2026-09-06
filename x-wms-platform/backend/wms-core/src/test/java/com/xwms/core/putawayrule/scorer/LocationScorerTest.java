package com.xwms.core.putawayrule.scorer;

import static org.junit.jupiter.api.Assertions.*;

import com.xwms.core.putawayrule.dto.PutawayRecommendContext;
import com.xwms.core.putawayrule.service.LocationQueryService;
import com.xwms.core.putawayrule.service.LocationQueryService.PutawayLocation;
import com.xwms.core.putawayrule.scorer.LocationScorer.ScoredLocation;
import com.xwms.core.support.BaseTest;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

/** 库位排序打分器单元测试 验证 scoreAndSort 公开入口：欧氏距离 + 四维权重打分（距离40%/容量30%/周转20%/混放10%） */
class LocationScorerTest extends BaseTest {

    @Mock private LocationQueryService locationQueryService;

    private LocationScorer locationScorer;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        locationScorer = new LocationScorer(locationQueryService);
        // LocationScorer 的权重通过 @Value 注入，纯 Mockito 无 Spring 容器，用反射注入默认权重
        ReflectionTestUtils.setField(locationScorer, "distanceWeight", 0.4);
        ReflectionTestUtils.setField(locationScorer, "capacityWeight", 0.3);
        ReflectionTestUtils.setField(locationScorer, "cycleWeight", 0.2);
        ReflectionTestUtils.setField(locationScorer, "mixWeight", 0.1);
    }

    private PutawayLocation buildLocation(String code, int x, int y, int z) {
        PutawayLocation loc = new PutawayLocation();
        loc.setLocationCode(code);
        loc.setCoordX(x);
        loc.setCoordY(y);
        loc.setCoordZ(z);
        loc.setCapacity(new BigDecimal("1000"));
        loc.setUsedCapacity(BigDecimal.ZERO);
        loc.setAreaCode("A");
        loc.setSortNo(1);
        return loc;
    }

    private PutawayRecommendContext buildContext(String skuCode, String cycleLevel) {
        PutawayRecommendContext ctx = new PutawayRecommendContext();
        ctx.setSkuCode(skuCode);
        ctx.setCycleLevel(cycleLevel);
        return ctx;
    }

    @Test
    @DisplayName("计算两点间欧氏距离 - 3-4-5三角形")
    void testCalculateDistance_345Triangle() {
        PutawayLocation from = buildLocation("FROM", 0, 0, 0);
        PutawayLocation to = buildLocation("TO", 3, 4, 0);
        double distance = locationScorer.calculateDistance(to, from);
        assertEquals(5.0, distance, 0.001);
    }

    @Test
    @DisplayName("计算两点间欧氏距离 - 三维3-4-12")
    void testCalculateDistance_3D() {
        PutawayLocation from = buildLocation("FROM", 1, 2, 3);
        PutawayLocation to = buildLocation("TO", 4, 6, 15);
        double distance = locationScorer.calculateDistance(to, from);
        assertEquals(13.0, distance, 0.001);
    }

    @Test
    @DisplayName("计算两点间欧氏距离 - 同点距离为0")
    void testCalculateDistance_SamePoint() {
        PutawayLocation from = buildLocation("P", 5, 5, 5);
        PutawayLocation to = buildLocation("P2", 5, 5, 5);
        double distance = locationScorer.calculateDistance(to, from);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    @DisplayName("坐标缺失时回退到 sortNo 距离")
    void testCalculateDistance_NullCoordsFallbackSortNo() {
        PutawayLocation from = buildLocation("FROM", 0, 0, 0);
        PutawayLocation to = new PutawayLocation();
        to.setLocationCode("TO");
        to.setSortNo(77);
        // 坐标全 null，应回退 sortNo=77
        double distance = locationScorer.calculateDistance(to, from);
        assertEquals(77.0, distance, 0.001);
    }

    @Test
    @DisplayName("scoreAndSort - 距离近的库位综合得分更高")
    void testScoreAndSort_DistancePriority() {
        PutawayLocation base = buildLocation("BASE", 0, 0, 0);
        PutawayLocation near = buildLocation("NEAR", 1, 0, 0);
        PutawayLocation far = buildLocation("FAR", 50, 0, 0);
        PutawayRecommendContext ctx = buildContext("SKU001", "A");

        List<ScoredLocation> result =
                locationScorer.scoreAndSort(Arrays.asList(far, near), ctx, base);

        assertEquals(2, result.size());
        // 距离近的库位应排在前面（score 更高）
        assertEquals("NEAR", result.get(0).getLocation().getLocationCode());
        assertEquals("FAR", result.get(1).getLocation().getLocationCode());
        assertTrue(result.get(0).getScore() > result.get(1).getScore());
        // 验证距离分量
        assertEquals(1.0, result.get(0).getDistance(), 0.001);
        assertEquals(50.0, result.get(1).getDistance(), 0.001);
    }

    @Test
    @DisplayName("scoreAndSort - 空列表返回空结果")
    void testScoreAndSort_EmptyList() {
        PutawayLocation base = buildLocation("BASE", 0, 0, 0);
        PutawayRecommendContext ctx = buildContext("SKU001", "A");
        List<ScoredLocation> result = locationScorer.scoreAndSort(List.of(), ctx, base);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("scoreAndSort - null 列表返回空结果")
    void testScoreAndSort_NullList() {
        PutawayLocation base = buildLocation("BASE", 0, 0, 0);
        PutawayRecommendContext ctx = buildContext("SKU001", "A");
        List<ScoredLocation> result = locationScorer.scoreAndSort(null, ctx, base);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("scoreAndSort - 容量利用率低的库位容量得分更高")
    void testScoreAndSort_CapacityScore() {
        PutawayLocation base = buildLocation("BASE", 0, 0, 0);
        // 两库位距离相同，但利用率不同
        PutawayLocation low = buildLocation("LOW", 10, 0, 0);
        low.setUsedCapacity(new BigDecimal("100")); // 10% 利用率
        PutawayLocation high = buildLocation("HIGH", 10, 0, 0);
        high.setUsedCapacity(new BigDecimal("900")); // 90% 利用率
        PutawayRecommendContext ctx = buildContext("SKU001", "A");

        List<ScoredLocation> result =
                locationScorer.scoreAndSort(Arrays.asList(low, high), ctx, base);

        // 距离相同、cycle/mix 相同，容量利用率低者得分高（低利用率 LOW=90 分 > 高利用率 HIGH=10 分）
        assertTrue(result.get(0).getCapacityScore() > result.get(1).getCapacityScore());
        assertEquals("LOW", result.get(0).getLocation().getLocationCode());
    }

    @Test
    @DisplayName("scoreAndSort - 周转级别匹配库区得分更高")
    void testScoreAndSort_CycleScore() {
        PutawayLocation base = buildLocation("BASE", 0, 0, 0);
        // 两库位距离相同，库区不同
        PutawayLocation areaA = buildLocation("A-LOC", 10, 0, 0);
        areaA.setAreaCode("A");
        PutawayLocation areaC = buildLocation("C-LOC", 10, 0, 0);
        areaC.setAreaCode("C");
        PutawayRecommendContext ctx = buildContext("SKU001", "A"); // 周转级别 A

        List<ScoredLocation> result =
                locationScorer.scoreAndSort(Arrays.asList(areaC, areaA), ctx, base);

        // A 区匹配 cycleLevel=A 得分高，C 区不匹配得分低
        assertTrue(result.get(0).getCycleScore() > result.get(1).getCycleScore());
        assertEquals("A-LOC", result.get(0).getLocation().getLocationCode());
    }

    @Test
    @DisplayName("scoreAndSort - 混放相同 SKU 得分高于空库位")
    void testScoreAndSort_MixScore() {
        PutawayLocation base = buildLocation("BASE", 0, 0, 0);
        // 两库位距离相同，一个已有相同 SKU，一个为空
        PutawayLocation sameSku = buildLocation("SAME", 10, 0, 0);
        sameSku.setCurrentSku("SKU001");
        PutawayLocation empty = buildLocation("EMPTY", 10, 0, 0);
        empty.setCurrentSku(null);
        PutawayRecommendContext ctx = buildContext("SKU001", "A");

        List<ScoredLocation> result =
                locationScorer.scoreAndSort(Arrays.asList(empty, sameSku), ctx, base);

        // 相同 SKU 库位 mixScore=100，空库位 mixScore=50
        assertTrue(result.get(0).getMixScore() > result.get(1).getMixScore());
        assertEquals("SAME", result.get(0).getLocation().getLocationCode());
        assertEquals(100.0, result.get(0).getMixScore(), 0.001);
        assertEquals(50.0, result.get(1).getMixScore(), 0.001);
    }

    @Test
    @DisplayName("scoreAndSort - ScoredLocation 含全部维度分量")
    void testScoreAndSort_ScoredLocationFields() {
        PutawayLocation base = buildLocation("BASE", 0, 0, 0);
        PutawayLocation loc = buildLocation("LOC", 5, 0, 0);
        loc.setUsedCapacity(new BigDecimal("500"));
        loc.setAreaCode("A");
        loc.setCurrentSku("SKU001");
        PutawayRecommendContext ctx = buildContext("SKU001", "A");

        List<ScoredLocation> result = locationScorer.scoreAndSort(List.of(loc), ctx, base);

        assertEquals(1, result.size());
        ScoredLocation scored = result.get(0);
        assertNotNull(scored.getLocation());
        assertEquals(5.0, scored.getDistance(), 0.001);
        // maxDistance=5, distanceScore=(1-5/5)*100=0
        assertEquals(0.0, scored.getDistanceScore(), 0.001);
        // capacityScore=(1-500/1000)*100=50
        assertEquals(50.0, scored.getCapacityScore(), 0.001);
        // cycleLevel=A, areaCode=A → 100
        assertEquals(100.0, scored.getCycleScore(), 0.001);
        // currentSku=SKU001 == ctx.skuCode → 100
        assertEquals(100.0, scored.getMixScore(), 0.001);
        // totalScore = 0*0.4 + 50*0.3 + 100*0.2 + 100*0.1 = 0+15+20+10 = 45
        assertEquals(45.0, scored.getScore(), 0.001);
    }
}
