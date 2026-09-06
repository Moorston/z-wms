package com.xwms.core.expiry.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.config.service.SysConfigService;
import com.xwms.core.expiry.entity.*;
import com.xwms.core.expiry.mapper.*;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 效期管控流程深化服务 核心能力: 入库效期拦截/出库效期预判/收货人效期差异/生产日期自动计算失效日期 适用行业: 食品、医药、化工等有效期敏感产品 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpiryProcessService {

    private final ExpiryRuleMapper expiryRuleMapper;
    private final ExpiryBatchMapper expiryBatchMapper;
    private final ExpiryAlertMapper expiryAlertMapper;
    private final CustomerExpiryMapper customerExpiryMapper;
    private final SysConfigService sysConfigService;

    // ============================================================

    // 1. 入库效期拦截
    // ============================================================

    /**
     * 入库效期校验 超过入库效期→拦截，不可收货
     *
     * @param skuCode 商品编码
     * @param productionDate 生产日期
     * @param expiryDate 失效日期
     * @param inboundDate 入库日期
     * @return 校验结果
     */
    public ExpiryCheckResult checkInboundExpiry(
            String skuCode,
            LocalDateTime productionDate,
            LocalDateTime expiryDate,
            LocalDateTime inboundDate) {
        log.info(
                "入库效期校验: sku={}, 生产={}, 失效={}, 入库={}",
                skuCode,
                productionDate,
                expiryDate,
                inboundDate);

        ExpiryCheckResult result = new ExpiryCheckResult();
        result.setSkuCode(skuCode);
        result.setCheckType("INBOUND");

        // 1. 基础校验：失效日期必须大于生产日期
        if (productionDate != null && expiryDate != null && expiryDate.isBefore(productionDate)) {
            result.setPassed(false);
            result.setLevel("ERROR");
            result.setMessage("失效日期不能早于生产日期");
            return result;
        }

        // 2. 超期产品校验：失效日期早于入库日期→拦截
        if (expiryDate != null && expiryDate.toLocalDate().isBefore(inboundDate.toLocalDate())) {
            result.setPassed(false);
            result.setLevel("ERROR");
            result.setMessage("产品已过期，失效日期=" + expiryDate.toLocalDate() + "，不可入库");
            result.setExpired(true);
            return result;
        }

        // 3. 获取商品效期规则
        ExpiryRule rule = getExpiryRule(skuCode);
        if (rule == null) {
            result.setPassed(true);
            result.setLevel("INFO");
            result.setMessage("未配置效期规则，跳过效期校验");
            return result;
        }

        // 4. 入库效期校验：入库后安全存放最大天数
        if (rule.getInboundExpiryDays() != null && rule.getInboundExpiryDays() > 0) {
            if (expiryDate != null) {
                long remainingDays =
                        ChronoUnit.DAYS.between(
                                inboundDate.toLocalDate(), expiryDate.toLocalDate());
                result.setRemainingDays(remainingDays);
                if (remainingDays < rule.getInboundExpiryDays()) {
                    result.setPassed(false);
                    result.setLevel("ERROR");
                    result.setMessage(
                            "入库效期不足，剩余"
                                    + remainingDays
                                    + "天，要求最少"
                                    + rule.getInboundExpiryDays()
                                    + "天");
                    result.setInboundExpiryBlocked(true);
                    return result;
                }
            }
        }

        // 5. 入库效期预警
        if (rule.getInboundWarningDays() != null
                && rule.getInboundWarningDays() > 0
                && expiryDate != null) {
            long remainingDays =
                    ChronoUnit.DAYS.between(inboundDate.toLocalDate(), expiryDate.toLocalDate());
            if (remainingDays <= rule.getInboundWarningDays()) {
                result.setPassed(true);
                result.setLevel("WARNING");
                result.setMessage("入库效期预警，剩余" + remainingDays + "天，请关注");
                result.setWarning(true);
                // 创建效期预警记录
                createExpiryAlert(skuCode, null, expiryDate, "INBOUND_WARNING", remainingDays);
                return result;
            }
        }

        result.setPassed(true);
        result.setLevel("INFO");
        result.setMessage("入库效期校验通过");
        if (expiryDate != null) {
            long remainingDays =
                    ChronoUnit.DAYS.between(inboundDate.toLocalDate(), expiryDate.toLocalDate());
            result.setRemainingDays(remainingDays);
        }
        return result;
    }

    // ============================================================

    // 2. 出库效期预判
    // ============================================================

    /**
     * 出库效期预判 低于出库效期→预警提示
     *
     * @param skuCode 商品编码
     * @param expiryDate 失效日期
     * @param outboundDate 出库日期
     * @param customerCode 收货人编码（用于收货人效期差异）
     * @return 校验结果
     */
    public ExpiryCheckResult checkOutboundExpiry(
            String skuCode,
            LocalDateTime expiryDate,
            LocalDateTime outboundDate,
            String customerCode) {
        log.info(
                "出库效期预判: sku={}, 失效={}, 出库={}, 客户={}",
                skuCode,
                expiryDate,
                outboundDate,
                customerCode);

        ExpiryCheckResult result = new ExpiryCheckResult();
        result.setSkuCode(skuCode);
        result.setCheckType("OUTBOUND");

        if (expiryDate == null) {
            result.setPassed(true);
            result.setLevel("INFO");
            result.setMessage("无失效日期，跳过效期校验");
            return result;
        }

        // 1. 已过期校验
        if (expiryDate.toLocalDate().isBefore(outboundDate.toLocalDate())) {
            result.setPassed(false);
            result.setLevel("ERROR");
            result.setMessage("产品已过期，失效日期=" + expiryDate.toLocalDate() + "，不可出库");
            result.setExpired(true);
            return result;
        }

        long remainingDays =
                ChronoUnit.DAYS.between(outboundDate.toLocalDate(), expiryDate.toLocalDate());
        result.setRemainingDays(remainingDays);

        // 2. 收货人效期差异校验（优先）
        if (customerCode != null) {
            CustomerExpiry customerExpiry =
                    customerExpiryMapper.selectByCustomerAndSku(customerCode, skuCode);
            if (customerExpiry != null && "Y".equals(customerExpiry.getEnabled())) {
                // 收货人出库效期要求
                if (customerExpiry.getOutboundExpiryDays() != null
                        && customerExpiry.getOutboundExpiryDays() > 0) {
                    if (remainingDays < customerExpiry.getOutboundExpiryDays()) {
                        String handleType = customerExpiry.getInsufficientHandleType();
                        if ("REJECT".equals(handleType)) {
                            result.setPassed(false);
                            result.setLevel("ERROR");
                            result.setMessage(
                                    "收货人"
                                            + customerCode
                                            + "要求出库效期最少"
                                            + customerExpiry.getOutboundExpiryDays()
                                            + "天，当前剩余"
                                            + remainingDays
                                            + "天，不可出库");
                            result.setCustomerExpiryBlocked(true);
                            return result;
                        } else if ("WARNING".equals(handleType)) {
                            result.setPassed(true);
                            result.setLevel("WARNING");
                            result.setMessage(
                                    "收货人"
                                            + customerCode
                                            + "要求出库效期最少"
                                            + customerExpiry.getOutboundExpiryDays()
                                            + "天，当前剩余"
                                            + remainingDays
                                            + "天，请确认");
                            result.setWarning(true);
                            return result;
                        }
                    }
                }
                // 收货人效期校验通过
                result.setPassed(true);
                result.setLevel("INFO");
                result.setMessage("出库效期校验通过（收货人要求），剩余" + remainingDays + "天");
                return result;
            }
        }

        // 3. 商品通用出库效期校验
        ExpiryRule rule = getExpiryRule(skuCode);
        if (rule != null) {
            // 出库效期要求
            if (rule.getOutboundExpiryDays() != null && rule.getOutboundExpiryDays() > 0) {
                if (remainingDays < rule.getOutboundExpiryDays()) {
                    result.setPassed(false);
                    result.setLevel("ERROR");
                    result.setMessage(
                            "出库效期不足，剩余"
                                    + remainingDays
                                    + "天，要求最少"
                                    + rule.getOutboundExpiryDays()
                                    + "天");
                    result.setOutboundExpiryBlocked(true);
                    return result;
                }
            }
            // 出库效期预警
            if (rule.getOutboundWarningDays() != null && rule.getOutboundWarningDays() > 0) {
                if (remainingDays <= rule.getOutboundWarningDays()) {
                    result.setPassed(true);
                    result.setLevel("WARNING");
                    result.setMessage("出库效期预警，剩余" + remainingDays + "天，请关注");
                    result.setWarning(true);
                    createExpiryAlert(skuCode, null, expiryDate, "OUTBOUND_WARNING", remainingDays);
                    return result;
                }
            }
        }

        result.setPassed(true);
        result.setLevel("INFO");
        result.setMessage("出库效期校验通过，剩余" + remainingDays + "天");
        return result;
    }

    // ============================================================

    // 3. 根据生产日期自动计算失效日期（MDT_EDT_CAL参数）
    // ============================================================

    /**
     * 根据生产日期和保质期自动计算失效日期
     *
     * @param skuCode 商品编码
     * @param productionDate 生产日期
     * @return 失效日期
     */
    public LocalDateTime calculateExpiryDate(String skuCode, LocalDateTime productionDate) {
        log.info("计算失效日期: sku={}, 生产={}", skuCode, productionDate);

        // 检查是否开启自动计算
        if (!sysConfigService.getBooleanConfig(SysConfigService.MDT_EDT_CAL)) {
            log.info("未开启生产日期自动计算失效日期（MDT_EDT_CAL=N）");
            return null;
        }

        if (productionDate == null) {
            return null;
        }

        // 获取商品效期规则（保质期）
        ExpiryRule rule = getExpiryRule(skuCode);
        if (rule == null || rule.getShelfLifeDays() == null || rule.getShelfLifeDays() <= 0) {
            log.warn("商品未配置保质期，无法自动计算失效日期: sku={}", skuCode);
            return null;
        }

        // 生产日期 + 保质期 = 失效日期
        LocalDateTime expiryDate = productionDate.plusDays(rule.getShelfLifeDays());
        log.info("失效日期计算完成: 生产={} + {}天 = {}", productionDate, rule.getShelfLifeDays(), expiryDate);
        return expiryDate;
    }

    // ============================================================

    // 4. 收货人效期管理
    // ============================================================

    /** 获取收货人效期要求 */
    public CustomerExpiry getCustomerExpiry(String customerCode, String skuCode) {
        return customerExpiryMapper.selectByCustomerAndSku(customerCode, skuCode);
    }

    /** 查询收货人所有效期要求 */
    public List<CustomerExpiry> getCustomerExpiries(String customerCode) {
        return customerExpiryMapper.selectByCustomer(customerCode);
    }

    /** 保存收货人效期要求 */
    @Transactional(rollbackFor = Exception.class)
    public CustomerExpiry saveCustomerExpiry(CustomerExpiry customerExpiry) {
        if (customerExpiry.getId() == null) {
            customerExpiry.setCreatedTime(LocalDateTime.now());
            customerExpiryMapper.insert(customerExpiry);
        } else {
            customerExpiry.setUpdatedTime(LocalDateTime.now());
            customerExpiryMapper.updateById(customerExpiry);
        }
        return customerExpiry;
    }

    // ============================================================

    // 5. 效期预警扫描（定时任务调用）
    // ============================================================

    /** 扫描近效期库存，生成预警记录 */
    @Transactional(rollbackFor = Exception.class)
    public List<ExpiryAlert> scanExpiryAlerts(int warningDays) {
        log.info("扫描近效期库存: 预警天数={}", warningDays);

        List<ExpiryAlert> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(warningDays);

        // 查询所有在库效期批次（未过期销毁的）
        List<ExpiryBatch> batches =
                expiryBatchMapper.selectList(
                        new LambdaQueryWrapper<ExpiryBatch>()
                                .ne(ExpiryBatch::getExpiryStatus, "EXPIRED")
                                .isNotNull(ExpiryBatch::getExpiryDate));

        for (ExpiryBatch batch : batches) {
            if (batch.getExpiryDate() == null) continue;
            LocalDate expiryDate = batch.getExpiryDate();
            long remainingDays = ChronoUnit.DAYS.between(today, expiryDate);

            // 已过期
            if (remainingDays < 0) {
                ExpiryAlert alert =
                        createExpiryAlert(
                                batch.getSkuCode(),
                                batch.getBatchNo(),
                                batch.getExpiryDate().atStartOfDay(),
                                "EXPIRED",
                                remainingDays);
                alerts.add(alert);
            } else if (remainingDays <= warningDays) {
                String level =
                        remainingDays <= 7 ? "CRITICAL" : remainingDays <= 30 ? "WARNING" : "INFO";
                ExpiryAlert alert =
                        createExpiryAlert(
                                batch.getSkuCode(),
                                batch.getBatchNo(),
                                batch.getExpiryDate().atStartOfDay(),
                                "NEAR_EXPIRY_" + level,
                                remainingDays);
                alerts.add(alert);
            }
        }

        log.info("效期预警扫描完成: 生成预警{}条", alerts.size());
        return alerts;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    /** 获取商品效期规则 */
    private ExpiryRule getExpiryRule(String skuCode) {
        return expiryRuleMapper.selectOne(
                new LambdaQueryWrapper<ExpiryRule>()
                        .eq(ExpiryRule::getSkuCode, skuCode)
                        .eq(ExpiryRule::getStatus, "ACTIVE")
                        .last("LIMIT 1"));
    }

    /** 创建效期预警记录 */
    private ExpiryAlert createExpiryAlert(
            String skuCode,
            String batchNo,
            LocalDateTime expiryDate,
            String alertType,
            long remainingDays) {
        ExpiryAlert alert = new ExpiryAlert();
        alert.setAlertNo("EA" + System.currentTimeMillis() + (int) (Math.random() * 1000));
        alert.setSkuCode(skuCode);
        alert.setBatchNo(batchNo);
        alert.setExpiryDate(expiryDate != null ? expiryDate.toLocalDate() : null);
        alert.setAlertType(alertType);
        alert.setRemainDays((int) remainingDays);
        alert.setStatus("PENDING");
        // createdTime 由 @TableField(fill = INSERT) 自动填充，作为预警生成时间
        expiryAlertMapper.insert(alert);
        return alert;
    }

    /** 效期校验结果内部类 */
    @Data
    public static class ExpiryCheckResult {
        private String skuCode;
        private String checkType; // INBOUND/OUTBOUND
        private boolean passed;
        private String level; // ERROR/WARNING/INFO
        private String message;
        private long remainingDays;
        private boolean expired;
        private boolean warning;
        private boolean inboundExpiryBlocked;
        private boolean outboundExpiryBlocked;
        private boolean customerExpiryBlocked;
    }
}
