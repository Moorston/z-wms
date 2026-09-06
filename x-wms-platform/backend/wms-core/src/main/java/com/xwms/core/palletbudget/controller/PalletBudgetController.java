package com.xwms.core.palletbudget.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.palletbudget.entity.PalletBudget;
import com.xwms.core.palletbudget.service.PalletBudgetService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pallet-budget")
@RequiredArgsConstructor
public class PalletBudgetController {

    private final PalletBudgetService palletBudgetService;

    @PostMapping("/execute")
    public PalletBudget executeBudget(
            @RequestParam String asnNo,
            @RequestParam String skuCode,
            @RequestParam String skuName,
            @RequestParam BigDecimal totalQty,
            @RequestParam Integer boxQty,
            @RequestParam(required = false) String locationCode,
            @RequestParam(required = false) BigDecimal locationHeight,
            @RequestParam(required = false) BigDecimal boxHeight,
            @RequestParam(required = false) BigDecimal boxWeight,
            @RequestParam(required = false) Integer boxesPerLayer,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return palletBudgetService.executeBudget(
                asnNo,
                skuCode,
                skuName,
                totalQty,
                boxQty,
                locationCode,
                locationHeight,
                boxHeight,
                boxWeight,
                boxesPerLayer,
                operator);
    }

    @PostMapping("/{budgetNo}/prepare")
    public PalletBudget preparePallet(
            @PathVariable String budgetNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        return palletBudgetService.preparePallet(budgetNo, operator);
    }

    @GetMapping("/{budgetNo}/check-full")
    public Map<String, Object> checkFullPallet(
            @PathVariable String budgetNo, @RequestParam int currentBoxes) {
        boolean full = palletBudgetService.checkFullPallet(budgetNo, currentBoxes);
        return Map.of("budgetNo", budgetNo, "currentBoxes", currentBoxes, "isFull", full);
    }

    @GetMapping("/asn/{asnNo}")
    public List<PalletBudget> getBudgetsByAsn(@PathVariable String asnNo) {
        return palletBudgetService.getBudgetsByAsn(asnNo);
    }
}
