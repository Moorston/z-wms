package com.xwms.core.plugin.industry.gsp.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/** 医药GSP批号效期管理服务 核心能力： 1. 近效期先出（FEFO）分配 2. 效期预警（30/60/90天） 3. 批号唯一性校验 4. 首营品种审核 */
@Slf4j
@Service
public class GspBatchService {

    /** 近效期预警阈值（天） */
    private static final int ALERT_NEAR = 30;

    private static final int ALERT_WARNING = 60;
    private static final int ALERT_ATTENTION = 90;

    /** FEFO近效期先出排序 将库存按效期升序排列，优先分配近效期批次 */
    public List<Map<String, Object>> sortByFefo(List<Map<String, Object>> inventoryList) {
        inventoryList.sort(
                (a, b) -> {
                    LocalDate expA = LocalDate.parse(a.get("expireDate").toString());
                    LocalDate expB = LocalDate.parse(b.get("expireDate").toString());
                    return expA.compareTo(expB);
                });
        return inventoryList;
    }

    /**
     * 效期预警检查
     *
     * @return 预警级别：NORMAL/ATTENTION(90天)/WARNING(60天)/NEAR(30天)/EXPIRED(已过期)
     */
    public String checkExpireAlert(LocalDate expireDate) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), expireDate);
        if (days < 0) return "EXPIRED";
        if (days <= ALERT_NEAR) return "NEAR";
        if (days <= ALERT_WARNING) return "WARNING";
        if (days <= ALERT_ATTENTION) return "ATTENTION";
        return "NORMAL";
    }

    /** 批号唯一性校验 医药行业同一SKU+同一批号必须唯一，不允许重复收货 */
    public boolean validateBatchUnique(String sku, String batchNo, List<String> existingBatches) {
        if (existingBatches.contains(batchNo)) {
            log.warn("GSP批号重复: sku={}, batchNo={}", sku, batchNo);
            return false;
        }
        return true;
    }

    /** 首营品种审核检查 新供应商+新品种必须先通过首营审核才能入库 */
    public boolean checkFirstBusinessApproval(
            String supplierCode,
            String sku,
            Set<String> approvedSuppliers,
            Set<String> approvedSkus) {
        boolean supplierApproved = approvedSuppliers.contains(supplierCode);
        boolean skuApproved = approvedSkus.contains(sku);
        if (!supplierApproved) {
            log.warn("首营企业未审核: supplier={}", supplierCode);
            return false;
        }
        if (!skuApproved) {
            log.warn("首营品种未审核: sku={}", sku);
            return false;
        }
        return true;
    }

    /** 特殊药品管控检查 麻醉药品/精神药品/医疗用毒性药品/放射性药品需要特殊管控 */
    public boolean isSpecialDrug(String drugType) {
        return Arrays.asList("NARCOTIC", "PSYCHOTIC", "TOXIC", "RADIOACTIVE").contains(drugType);
    }

    /** 特殊药品双人复核要求 */
    public boolean needDoubleCheck(String drugType) {
        return isSpecialDrug(drugType);
    }
}
