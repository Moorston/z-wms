package com.xwms.core.combo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.combo.entity.*;
import com.xwms.core.combo.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 库存组合管理核心服务 核心能力: 组合规则/组合明细/组合组装/组合拆解/可用量计算 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryComboService {

    private final ComboRuleMapper ruleMapper;
    private final ComboItemMapper itemMapper;
    private final ComboAssembleMapper assembleMapper;
    private final ComboDisassembleMapper disassembleMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 组合规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ComboRule createRule(ComboRule rule, List<ComboItem> items) {
        rule.setStatus("ACTIVE");
        if (rule.getAutoAssemble() == null) rule.setAutoAssemble("N");
        if (rule.getAutoDisassemble() == null) rule.setAutoDisassemble("N");
        ruleMapper.insert(rule);

        if (items != null) {
            for (ComboItem item : items) {
                item.setComboCode(rule.getComboCode());
                if (item.getIsOptional() == null) item.setIsOptional("N");
                if (item.getSortNo() == null) item.setSortNo(0);
                itemMapper.insert(item);
            }
        }

        log.info(
                "创建组合规则: {}={}, items={}",
                rule.getComboCode(),
                rule.getComboName(),
                items != null ? items.size() : 0);
        return rule;
    }

    public ComboRule getRuleByCode(String comboCode) {
        return ruleMapper.selectByComboCode(comboCode);
    }

    public List<ComboItem> getItemsByComboCode(String comboCode) {
        return itemMapper.selectByComboCode(comboCode);
    }

    public List<ComboRule> getCombosByItemSku(String skuCode) {
        List<ComboItem> items = itemMapper.selectByItemSku(skuCode);
        List<ComboRule> rules = new ArrayList<>();
        for (ComboItem item : items) {
            ComboRule rule = ruleMapper.selectByComboCode(item.getComboCode());
            if (rule != null) rules.add(rule);
        }
        return rules;
    }

    public Page<ComboRule> pageRules(Page<ComboRule> page, String warehouseCode, String comboType) {
        LambdaQueryWrapper<ComboRule> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ComboRule::getWarehouseCode, warehouseCode);
        if (comboType != null) wrapper.eq(ComboRule::getComboType, comboType);
        wrapper.eq(ComboRule::getStatus, "ACTIVE");
        return ruleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 组合成本计算
    // ============================================================

    /** 计算组合品成本 */
    public BigDecimal calculateComboCost(String comboCode) {
        ComboRule rule = ruleMapper.selectByComboCode(comboCode);
        if (rule == null) throw new RuntimeException("组合规则不存在: " + comboCode);

        if ("FIXED".equals(rule.getCostCalcMethod()) && rule.getFixedCost() != null) {
            return rule.getFixedCost();
        }

        List<ComboItem> items = itemMapper.selectByComboCode(comboCode);
        BigDecimal totalCost = BigDecimal.ZERO;
        for (ComboItem item : items) {
            if ("Y".equals(item.getIsOptional())) continue;
            if (item.getUnitCost() != null && item.getQuantity() != null) {
                totalCost = totalCost.add(item.getUnitCost().multiply(item.getQuantity()));
            }
        }
        return totalCost;
    }

    // ============================================================

    // 3. 组合可用量计算
    // ============================================================

    /** 计算组合品可组装数量 原理: 取所有子品可用数量 / 子品需求数量 的最小值 */
    public BigDecimal calculateAssemblableQty(
            String comboCode, Map<String, BigDecimal> itemInventory) {
        List<ComboItem> items = itemMapper.selectByComboCode(comboCode);
        if (items.isEmpty()) return BigDecimal.ZERO;

        BigDecimal minQty = null;
        for (ComboItem item : items) {
            if ("Y".equals(item.getIsOptional())) continue;
            BigDecimal available =
                    itemInventory.getOrDefault(item.getItemSkuCode(), BigDecimal.ZERO);
            BigDecimal required = item.getQuantity();
            if (required == null || required.compareTo(BigDecimal.ZERO) == 0) continue;
            BigDecimal assemblable = available.divide(required, 0, java.math.RoundingMode.DOWN);
            if (minQty == null || assemblable.compareTo(minQty) < 0) {
                minQty = assemblable;
            }
        }
        return minQty != null ? minQty : BigDecimal.ZERO;
    }

    // ============================================================

    // 4. 组合组装
    // ============================================================

    /** 创建组装单 */
    @Transactional(rollbackFor = Exception.class)
    public ComboAssemble createAssemble(
            String comboCode,
            String warehouseCode,
            String ownerCode,
            BigDecimal assembleQty,
            String sourceType,
            String sourceNo,
            String operator,
            String remark) {
        ComboRule rule = ruleMapper.selectByComboCode(comboCode);
        if (rule == null) throw new RuntimeException("组合规则不存在: " + comboCode);

        ComboAssemble assemble = new ComboAssemble();
        assemble.setAssembleNo(generateAssembleNo());
        assemble.setComboCode(comboCode);
        assemble.setComboName(rule.getComboName());
        assemble.setWarehouseCode(warehouseCode);
        assemble.setOwnerCode(ownerCode);
        assemble.setAssembleQty(assembleQty);
        assemble.setStatus("PENDING");
        assemble.setSourceType(sourceType);
        assemble.setSourceNo(sourceNo);
        assemble.setOperator(operator);
        assemble.setRemark(remark);
        assembleMapper.insert(assemble);

        log.info(
                "创建组装单: assembleNo={}, combo={}, qty={}",
                assemble.getAssembleNo(),
                comboCode,
                assembleQty);
        return assemble;
    }

    /** 执行组装 原理: 扣减子品库存，增加组合品库存 */
    @Transactional(rollbackFor = Exception.class)
    public ComboAssemble executeAssemble(String assembleNo) {
        ComboAssemble assemble = assembleMapper.selectByAssembleNo(assembleNo);
        if (assemble == null) throw new RuntimeException("组装单不存在: " + assembleNo);
        if (!"PENDING".equals(assemble.getStatus())) {
            throw new RuntimeException("组装单状态不正确: " + assemble.getStatus());
        }

        assembleMapper.updateStatus(assembleNo, "PROCESSING");

        // TODO: 扣减子品库存，增加组合品库存
        // 需要调用库存服务: 子品出库 + 组合品入库

        assembleMapper.updateStatus(assembleNo, "COMPLETED");

        log.info(
                "执行组装完成: assembleNo={}, combo={}, qty={}",
                assembleNo,
                assemble.getComboCode(),
                assemble.getAssembleQty());
        return assembleMapper.selectByAssembleNo(assembleNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public ComboAssemble cancelAssemble(String assembleNo, String reason) {
        ComboAssemble assemble = assembleMapper.selectByAssembleNo(assembleNo);
        if (assemble == null) throw new RuntimeException("组装单不存在: " + assembleNo);
        if (!"PENDING".equals(assemble.getStatus())) {
            throw new RuntimeException("只有待处理状态可取消: " + assemble.getStatus());
        }
        assembleMapper.updateStatus(assembleNo, "CANCELLED");
        log.info("取消组装单: assembleNo={}, reason={}", assembleNo, reason);
        return assembleMapper.selectByAssembleNo(assembleNo);
    }

    // ============================================================

    // 5. 组合拆解
    // ============================================================

    /** 创建拆解单 */
    @Transactional(rollbackFor = Exception.class)
    public ComboDisassemble createDisassemble(
            String comboCode,
            String warehouseCode,
            String ownerCode,
            BigDecimal disassembleQty,
            String sourceType,
            String sourceNo,
            String operator,
            String remark) {
        ComboRule rule = ruleMapper.selectByComboCode(comboCode);
        if (rule == null) throw new RuntimeException("组合规则不存在: " + comboCode);

        ComboDisassemble disassemble = new ComboDisassemble();
        disassemble.setDisassembleNo(generateDisassembleNo());
        disassemble.setComboCode(comboCode);
        disassemble.setComboName(rule.getComboName());
        disassemble.setWarehouseCode(warehouseCode);
        disassemble.setOwnerCode(ownerCode);
        disassemble.setDisassembleQty(disassembleQty);
        disassemble.setStatus("PENDING");
        disassemble.setSourceType(sourceType);
        disassemble.setSourceNo(sourceNo);
        disassemble.setOperator(operator);
        disassemble.setRemark(remark);
        disassembleMapper.insert(disassemble);

        log.info(
                "创建拆解单: disassembleNo={}, combo={}, qty={}",
                disassemble.getDisassembleNo(),
                comboCode,
                disassembleQty);
        return disassemble;
    }

    /** 执行拆解 原理: 扣减组合品库存，增加子品库存 */
    @Transactional(rollbackFor = Exception.class)
    public ComboDisassemble executeDisassemble(String disassembleNo) {
        ComboDisassemble disassemble = disassembleMapper.selectByDisassembleNo(disassembleNo);
        if (disassemble == null) throw new RuntimeException("拆解单不存在: " + disassembleNo);
        if (!"PENDING".equals(disassemble.getStatus())) {
            throw new RuntimeException("拆解单状态不正确: " + disassemble.getStatus());
        }

        disassembleMapper.updateStatus(disassembleNo, "PROCESSING");

        // TODO: 扣减组合品库存，增加子品库存
        // 需要调用库存服务: 组合品出库 + 子品入库

        disassembleMapper.updateStatus(disassembleNo, "COMPLETED");

        log.info(
                "执行拆解完成: disassembleNo={}, combo={}, qty={}",
                disassembleNo,
                disassemble.getComboCode(),
                disassemble.getDisassembleQty());
        return disassembleMapper.selectByDisassembleNo(disassembleNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public ComboDisassemble cancelDisassemble(String disassembleNo, String reason) {
        ComboDisassemble disassemble = disassembleMapper.selectByDisassembleNo(disassembleNo);
        if (disassemble == null) throw new RuntimeException("拆解单不存在: " + disassembleNo);
        if (!"PENDING".equals(disassemble.getStatus())) {
            throw new RuntimeException("只有待处理状态可取消: " + disassemble.getStatus());
        }
        disassembleMapper.updateStatus(disassembleNo, "CANCELLED");
        log.info("取消拆解单: disassembleNo={}, reason={}", disassembleNo, reason);
        return disassembleMapper.selectByDisassembleNo(disassembleNo);
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public ComboAssemble getAssembleByNo(String assembleNo) {
        return assembleMapper.selectByAssembleNo(assembleNo);
    }

    public ComboDisassemble getDisassembleByNo(String disassembleNo) {
        return disassembleMapper.selectByDisassembleNo(disassembleNo);
    }

    public Page<ComboAssemble> pageAssembles(
            Page<ComboAssemble> page, String warehouseCode, String comboCode, String status) {
        LambdaQueryWrapper<ComboAssemble> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ComboAssemble::getWarehouseCode, warehouseCode);
        if (comboCode != null) wrapper.eq(ComboAssemble::getComboCode, comboCode);
        if (status != null) wrapper.eq(ComboAssemble::getStatus, status);
        wrapper.orderByDesc(ComboAssemble::getCreatedTime);
        return assembleMapper.selectPage(page, wrapper);
    }

    public Page<ComboDisassemble> pageDisassembles(
            Page<ComboDisassemble> page, String warehouseCode, String comboCode, String status) {
        LambdaQueryWrapper<ComboDisassemble> wrapper = new LambdaQueryWrapper<>();
        if (warehouseCode != null) wrapper.eq(ComboDisassemble::getWarehouseCode, warehouseCode);
        if (comboCode != null) wrapper.eq(ComboDisassemble::getComboCode, comboCode);
        if (status != null) wrapper.eq(ComboDisassemble::getStatus, status);
        wrapper.orderByDesc(ComboDisassemble::getCreatedTime);
        return disassembleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateAssembleNo() {
        return "ASM"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateDisassembleNo() {
        return "DIS"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
