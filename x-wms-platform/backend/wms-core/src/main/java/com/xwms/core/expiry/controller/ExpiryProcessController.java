package com.xwms.core.expiry.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import com.xwms.core.expiry.entity.CustomerExpiry;
import com.xwms.core.expiry.entity.ExpiryAlert;
import com.xwms.core.expiry.service.ExpiryProcessService;

import lombok.RequiredArgsConstructor;

/** 效期管控流程深化Controller */
@RestController
@RequestMapping("/api/expiry/process")
@RequiredArgsConstructor
public class ExpiryProcessController {

    private final ExpiryProcessService expiryProcessService;

    // ==================== 入库效期拦截 ====================

    /** 入库效期校验 */
    @PostMapping("/inbound/check")
    public ExpiryProcessService.ExpiryCheckResult checkInboundExpiry(
            @RequestParam String skuCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime productionDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime expiryDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime inboundDate) {
        if (inboundDate == null) inboundDate = LocalDateTime.now();
        return expiryProcessService.checkInboundExpiry(
                skuCode, productionDate, expiryDate, inboundDate);
    }

    // ==================== 出库效期预判 ====================

    /** 出库效期预判 */
    @PostMapping("/outbound/check")
    public ExpiryProcessService.ExpiryCheckResult checkOutboundExpiry(
            @RequestParam String skuCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime expiryDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime outboundDate,
            @RequestParam(required = false) String customerCode) {
        if (outboundDate == null) outboundDate = LocalDateTime.now();
        return expiryProcessService.checkOutboundExpiry(
                skuCode, expiryDate, outboundDate, customerCode);
    }

    // ==================== 失效日期自动计算 ====================

    /** 根据生产日期自动计算失效日期 */
    @PostMapping("/calculate-expiry")
    public Map<String, Object> calculateExpiryDate(
            @RequestParam String skuCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    LocalDateTime productionDate) {
        LocalDateTime expiryDate =
                expiryProcessService.calculateExpiryDate(skuCode, productionDate);
        return Map.of(
                "skuCode", skuCode,
                "productionDate", productionDate,
                "expiryDate", expiryDate != null ? expiryDate : "未开启自动计算或未配置保质期");
    }

    // ==================== 收货人效期管理 ====================

    /** 获取收货人效期要求 */
    @GetMapping("/customer/{customerCode}")
    public CustomerExpiry getCustomerExpiry(
            @PathVariable String customerCode, @RequestParam(required = false) String skuCode) {
        return expiryProcessService.getCustomerExpiry(customerCode, skuCode);
    }

    /** 查询收货人所有效期要求 */
    @GetMapping("/customer/{customerCode}/all")
    public List<CustomerExpiry> getCustomerExpiries(@PathVariable String customerCode) {
        return expiryProcessService.getCustomerExpiries(customerCode);
    }

    /** 保存收货人效期要求 */
    @PostMapping("/customer/save")
    public CustomerExpiry saveCustomerExpiry(@RequestBody CustomerExpiry customerExpiry) {
        return expiryProcessService.saveCustomerExpiry(customerExpiry);
    }

    // ==================== 效期预警扫描 ====================

    /** 扫描近效期库存 */
    @PostMapping("/scan-alerts")
    public List<ExpiryAlert> scanExpiryAlerts(@RequestParam(defaultValue = "30") int warningDays) {
        return expiryProcessService.scanExpiryAlerts(warningDays);
    }
}
