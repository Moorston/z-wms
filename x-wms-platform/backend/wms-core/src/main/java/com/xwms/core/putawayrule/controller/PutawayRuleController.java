package com.xwms.core.putawayrule.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.core.Result;
import com.xwms.core.putawayrule.entity.PutawayDetail;
import com.xwms.core.putawayrule.entity.PutawayLog;
import com.xwms.core.putawayrule.entity.PutawayRule;
import com.xwms.core.putawayrule.entity.PutawayRuleLine;
import com.xwms.core.putawayrule.entity.PutawayStrategy;
import com.xwms.core.putawayrule.service.PutawayRuleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** 上架规则控制器 */
@Tag(name = "上架规则管理", description = "上架规则、策略、库位推荐、上架执行")
@RestController
@RequestMapping("/api/putaway-rule")
@RequiredArgsConstructor
public class PutawayRuleController {

    private final PutawayRuleService putawayRuleService;

    // ============================================================

    // 1. 上架规则管理
    // ============================================================

    @Operation(summary = "创建上架规则")
    @PostMapping("/rules")
    public Result<PutawayRule> createRule(@RequestBody PutawayRule rule) {
        return Result.success(putawayRuleService.createRule(rule));
    }

    @Operation(summary = "更新上架规则")
    @PutMapping("/rules/{id}")
    public Result<PutawayRule> updateRule(@PathVariable Long id, @RequestBody PutawayRule rule) {
        rule.setId(id);
        return Result.success(putawayRuleService.updateRule(rule));
    }

    @Operation(summary = "查询上架规则详情")
    @GetMapping("/rules/{id}")
    public Result<PutawayRule> getRule(@PathVariable Long id) {
        return Result.success(putawayRuleService.getRule(id));
    }

    @Operation(summary = "根据编码查询上架规则")
    @GetMapping("/rules/code/{ruleCode}")
    public Result<PutawayRule> getRuleByCode(@PathVariable String ruleCode) {
        return Result.success(putawayRuleService.getRuleByCode(ruleCode));
    }

    @Operation(summary = "分页查询上架规则")
    @GetMapping("/rules")
    public Result<Page<PutawayRule>> pageRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseCode) {
        return Result.success(
                putawayRuleService.pageRules(
                        new Page<>(page, size), ruleType, status, warehouseCode));
    }

    @Operation(summary = "查询所有启用的上架规则")
    @GetMapping("/rules/enabled")
    public Result<List<PutawayRule>> getAllEnabledRules() {
        return Result.success(putawayRuleService.getAllEnabledRules());
    }

    // ============================================================

    // 2. 上架策略管理
    // ============================================================

    @Operation(summary = "创建上架策略")
    @PostMapping("/strategies")
    public Result<PutawayStrategy> createStrategy(@RequestBody PutawayStrategy strategy) {
        return Result.success(putawayRuleService.createStrategy(strategy));
    }

    @Operation(summary = "查询上架策略详情")
    @GetMapping("/strategies/{id}")
    public Result<PutawayStrategy> getStrategy(@PathVariable Long id) {
        return Result.success(putawayRuleService.getStrategy(id));
    }

    @Operation(summary = "根据编码查询上架策略")
    @GetMapping("/strategies/code/{strategyCode}")
    public Result<PutawayStrategy> getStrategyByCode(@PathVariable String strategyCode) {
        return Result.success(putawayRuleService.getStrategyByCode(strategyCode));
    }

    @Operation(summary = "根据规则编码查询关联策略")
    @GetMapping("/strategies/rule/{ruleCode}")
    public Result<List<PutawayStrategy>> getStrategiesByRuleCode(@PathVariable String ruleCode) {
        return Result.success(putawayRuleService.getStrategiesByRuleCode(ruleCode));
    }

    @Operation(summary = "分页查询上架策略")
    @GetMapping("/strategies")
    public Result<Page<PutawayStrategy>> pageStrategies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String strategyType,
            @RequestParam(required = false) String status) {
        return Result.success(
                putawayRuleService.pageStrategies(new Page<>(page, size), strategyType, status));
    }

    // ============================================================

    // 3. 库位推荐
    // ============================================================

    @Operation(summary = "推荐上架库位")
    @PostMapping("/recommend")
    public Result<List<PutawayDetail>> recommendLocations(
            @RequestParam String skuCode,
            @RequestParam(required = false) String categoryCode,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam BigDecimal quantity) {
        return Result.success(
                putawayRuleService.recommendLocations(
                        skuCode, categoryCode, ownerCode, warehouseCode, batchNo, quantity));
    }

    // ============================================================

    // 4. 执行上架
    // ============================================================

    @Operation(summary = "执行上架")
    @PostMapping("/execute")
    public Result<PutawayLog> executePutaway(
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) String asnNo,
            @RequestParam String skuCode,
            @RequestParam(required = false) String batchNo,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam String sourceLocation,
            @RequestParam String targetLocation,
            @RequestParam BigDecimal quantity,
            @RequestParam String operator) {
        return Result.success(
                putawayRuleService.executePutaway(
                        inboundNo,
                        asnNo,
                        skuCode,
                        batchNo,
                        ownerCode,
                        warehouseCode,
                        sourceLocation,
                        targetLocation,
                        quantity,
                        operator));
    }

    // ============================================================

    // 5. 查询日志和详情
    // ============================================================

    @Operation(summary = "查询上架日志详情")
    @GetMapping("/logs/{id}")
    public Result<PutawayLog> getLog(@PathVariable Long id) {
        return Result.success(putawayRuleService.getLog(id));
    }

    @Operation(summary = "根据编号查询上架日志")
    @GetMapping("/logs/code/{logNo}")
    public Result<PutawayLog> getLogByNo(@PathVariable String logNo) {
        return Result.success(putawayRuleService.getLogByNo(logNo));
    }

    @Operation(summary = "根据入库单号查询上架日志")
    @GetMapping("/logs/inbound/{inboundNo}")
    public Result<List<PutawayLog>> getLogsByInboundNo(@PathVariable String inboundNo) {
        return Result.success(putawayRuleService.getLogsByInboundNo(inboundNo));
    }

    @Operation(summary = "分页查询上架日志")
    @GetMapping("/logs")
    public Result<Page<PutawayLog>> pageLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) String skuCode,
            @RequestParam(required = false) String status) {
        return Result.success(
                putawayRuleService.pageLogs(new Page<>(page, size), inboundNo, skuCode, status));
    }

    @Operation(summary = "根据日志编号查询上架详情")
    @GetMapping("/details/log/{logNo}")
    public Result<List<PutawayDetail>> getDetailsByLogNo(@PathVariable String logNo) {
        return Result.success(putawayRuleService.getDetailsByLogNo(logNo));
    }

    @Operation(summary = "根据入库单号查询上架详情")
    @GetMapping("/details/inbound/{inboundNo}")
    public Result<List<PutawayDetail>> getDetailsByInboundNo(@PathVariable String inboundNo) {
        return Result.success(putawayRuleService.getDetailsByInboundNo(inboundNo));
    }

    // ============================================================

    // 7. 规则行管理（规则链）
    // ============================================================

    @Operation(summary = "查询规则详情（含规则行）")
    @GetMapping("/rules/{id}/with-lines")
    public Result<PutawayRule> getRuleWithLines(@PathVariable Long id) {
        return Result.success(putawayRuleService.getRuleWithLines(id));
    }

    @Operation(summary = "查询规则的所有规则行")
    @GetMapping("/rules/{ruleId}/lines")
    public Result<List<PutawayRuleLine>> getRuleLines(@PathVariable Long ruleId) {
        return Result.success(putawayRuleService.getRuleLines(ruleId));
    }

    @Operation(summary = "创建规则行")
    @PostMapping("/rules/{ruleId}/lines")
    public Result<PutawayRuleLine> createRuleLine(
            @PathVariable Long ruleId, @RequestBody PutawayRuleLine line) {
        return Result.success(putawayRuleService.createRuleLine(ruleId, line));
    }

    @Operation(summary = "更新规则行")
    @PutMapping("/rules/{ruleId}/lines/{lineId}")
    public Result<PutawayRuleLine> updateRuleLine(
            @PathVariable Long ruleId,
            @PathVariable Long lineId,
            @RequestBody PutawayRuleLine line) {
        line.setId(lineId);
        return Result.success(putawayRuleService.updateRuleLine(ruleId, line));
    }

    @Operation(summary = "删除规则行")
    @DeleteMapping("/rules/{ruleId}/lines/{lineId}")
    public Result<Void> deleteRuleLine(@PathVariable Long ruleId, @PathVariable Long lineId) {
        putawayRuleService.deleteRuleLine(ruleId, lineId);
        return Result.success();
    }

    @Operation(summary = "批量保存规则行（全量替换）")
    @PostMapping("/rules/{ruleId}/lines/batch")
    public Result<List<PutawayRuleLine>> batchSaveRuleLines(
            @PathVariable Long ruleId, @RequestBody List<PutawayRuleLine> lines) {
        return Result.success(putawayRuleService.batchSaveRuleLines(ruleId, lines));
    }

    // ============================================================

    // 8. 规则复制/启用/停用/删除
    // ============================================================

    @Operation(summary = "复制规则（含规则行）")
    @PostMapping("/rules/{id}/copy")
    public Result<PutawayRule> copyRule(
            @PathVariable Long id, @RequestParam(required = false) String newRuleName) {
        return Result.success(putawayRuleService.copyRule(id, newRuleName));
    }

    @Operation(summary = "启用规则")
    @PostMapping("/rules/{id}/enable")
    public Result<PutawayRule> enableRule(@PathVariable Long id) {
        return Result.success(putawayRuleService.enableRule(id));
    }

    @Operation(summary = "停用规则")
    @PostMapping("/rules/{id}/disable")
    public Result<PutawayRule> disableRule(@PathVariable Long id) {
        return Result.success(putawayRuleService.disableRule(id));
    }

    @Operation(summary = "删除规则（含规则行）")
    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        putawayRuleService.deleteRule(id);
        return Result.success();
    }
}
