package com.xwms.core.palletbudget.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.palletbudget.entity.PalletBudget;
import com.xwms.core.palletbudget.mapper.PalletBudgetMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 码盘预算服务 客户现场有较为细致的分货堆码要求，库内存储位高度不同，要求放到不同库位的托盘堆码层数不同 与标准码盘统一按包装计算相比，能实现复杂组盘逻辑 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PalletBudgetService {

    private final PalletBudgetMapper palletBudgetMapper;

    /** 执行码盘预算 根据预设的库位高度、箱型对应、组盘规则计算码盘 */
    @Transactional(rollbackFor = Exception.class)
    public PalletBudget executeBudget(
            String asnNo,
            String skuCode,
            String skuName,
            BigDecimal totalQty,
            Integer boxQty,
            String locationCode,
            BigDecimal locationHeight,
            BigDecimal boxHeight,
            BigDecimal boxWeight,
            Integer boxesPerLayer,
            String operator) {
        log.info(
                "执行码盘预算: asn={}, sku={}, 总数量={}, 库位高度={}",
                asnNo,
                skuCode,
                totalQty,
                locationHeight);

        // 计算每层箱数（如果未指定，按箱尺寸估算）
        if (boxesPerLayer == null || boxesPerLayer <= 0) {
            boxesPerLayer = 10; // 默认值
        }

        // 计算每托盘层数（库位高度 / 箱高，向下取整，预留托盘高度）
        int layersPerPallet = 0;
        if (locationHeight != null
                && boxHeight != null
                && boxHeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal availableHeight =
                    locationHeight.subtract(new BigDecimal("150")); // 预留托盘高度150mm
            layersPerPallet = availableHeight.divide(boxHeight, 0, RoundingMode.DOWN).intValue();
            if (layersPerPallet < 1) layersPerPallet = 1;
        } else {
            layersPerPallet = 5; // 默认5层
        }

        // 计算每托盘箱数
        int boxesPerPallet = boxesPerLayer * layersPerPallet;

        // 计算托盘数
        int palletQty = (int) Math.ceil((double) boxQty / boxesPerPallet);

        // 创建码盘预算记录
        PalletBudget budget = new PalletBudget();
        budget.setBudgetNo("PB" + System.currentTimeMillis());
        budget.setAsnNo(asnNo);
        budget.setSkuCode(skuCode);
        budget.setSkuName(skuName);
        budget.setTotalQty(totalQty);
        budget.setBoxQty(boxQty);
        budget.setPalletQty(palletQty);
        budget.setLocationCode(locationCode);
        budget.setLocationHeight(locationHeight);
        budget.setLayersPerPallet(layersPerPallet);
        budget.setBoxesPerLayer(boxesPerLayer);
        budget.setBoxHeight(boxHeight);
        budget.setBoxWeight(boxWeight);
        budget.setStatus("BUDGETED");
        budget.setOperator(operator);
        budget.setBudgetTime(LocalDateTime.now());
        budget.setCreatedBy(operator);
        budget.setCreatedTime(LocalDateTime.now());
        palletBudgetMapper.insert(budget);

        log.info(
                "码盘预算完成: budgetNo={}, 托盘数={}, 每托{}层×{}箱={}箱",
                budget.getBudgetNo(),
                palletQty,
                layersPerPallet,
                boxesPerLayer,
                boxesPerPallet);
        return budget;
    }

    /** 码盘准备（RF） 输入ASN编号，扫描箱号，系统提示预分库位，显示箱型箱重等信息，显示分货进度 */
    public PalletBudget preparePallet(String budgetNo, String operator) {
        log.info("码盘准备: budgetNo={}", budgetNo);
        PalletBudget budget = palletBudgetMapper.selectByBudgetNo(budgetNo);
        if (budget == null) {
            throw new RuntimeException("码盘预算不存在: " + budgetNo);
        }
        // TODO: 扫描箱号，系统提示预分库位，显示分货进度
        return budget;
    }

    /** 满托提示 预分库位分货数量完成，系统提示此库位满托 */
    public boolean checkFullPallet(String budgetNo, int currentBoxes) {
        PalletBudget budget = palletBudgetMapper.selectByBudgetNo(budgetNo);
        if (budget == null) return false;
        int boxesPerPallet = budget.getBoxesPerLayer() * budget.getLayersPerPallet();
        return currentBoxes >= boxesPerPallet;
    }

    /** 查询码盘预算 */
    public List<PalletBudget> getBudgetsByAsn(String asnNo) {
        return palletBudgetMapper.selectList(
                new LambdaQueryWrapper<PalletBudget>()
                        .eq(asnNo != null, PalletBudget::getAsnNo, asnNo)
                        .orderByDesc(PalletBudget::getCreatedTime));
    }
}
