package com.xwms.core.alert.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.alert.entity.*;
import com.xwms.core.alert.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 搴撳瓨棰勮/鍛婅绠＄悊鏍稿績鏈嶅姟 鏍稿績鑳藉姏: 棰勮瑙勫垯/棰勮妫€鏌?棰勮璁板綍/棰勮澶勭悊/棰勮閫氱煡 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAlertService {

    private final AlertRuleMapper ruleMapper;
    private final AlertRecordMapper recordMapper;
    private final AlertHandleMapper handleMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 棰勮瑙勫垯绠＄悊
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public AlertRule createRule(AlertRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getCheckFrequency() == null) rule.setCheckFrequency("DAILY");
        if (rule.getAutoHandle() == null) rule.setAutoHandle("N");
        if (rule.getPriority() == null) rule.setPriority(5);
        ruleMapper.insert(rule);
        log.info(
                "鍒涘缓棰勮瑙勫垯: {}={}, type={}, level={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getAlertType(),
                rule.getAlertLevel());
        return rule;
    }

    public AlertRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    public List<AlertRule> getRulesByType(String alertType) {
        return ruleMapper.selectByAlertType(alertType);
    }

    public List<AlertRule> getRulesByWarehouse(String warehouseCode) {
        return ruleMapper.selectByWarehouse(warehouseCode);
    }

    public List<AlertRule> getRulesByFrequency(String frequency) {
        return ruleMapper.selectByFrequency(frequency);
    }

    public List<AlertRule> getRulesBySku(String skuCode) {
        return ruleMapper.selectBySku(skuCode);
    }

    public Page<AlertRule> pageRules(Page<AlertRule> page, String alertType, String warehouseCode) {
        LambdaQueryWrapper<AlertRule> wrapper = new LambdaQueryWrapper<>();
        if (alertType != null) wrapper.eq(AlertRule::getAlertType, alertType);
        if (warehouseCode != null) wrapper.eq(AlertRule::getWarehouseCode, warehouseCode);
        wrapper.eq(AlertRule::getStatus, "ACTIVE");
        wrapper.orderByDesc(AlertRule::getPriority);
        return ruleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 棰勮妫€鏌ワ紙鏍稿績锛?
    // ============================================================

    /** 鎵ц棰勮妫€鏌? */
    @Transactional(rollbackFor = Exception.class)
    public List<AlertRecord> checkAlerts(String frequency, Map<String, Object> inventoryData) {
        List<AlertRule> rules = ruleMapper.selectByFrequency(frequency);
        List<AlertRecord> triggeredAlerts = new ArrayList<>();

        for (AlertRule rule : rules) {
            // 妫€鏌ヨ鍒欐槸鍚︾敓鏁?
            if (rule.getEffectiveDate() != null
                    && rule.getEffectiveDate().isAfter(java.time.LocalDate.now())) continue;
            if (rule.getExpireDate() != null
                    && rule.getExpireDate().isBefore(java.time.LocalDate.now())) continue;

            // 鑾峰彇褰撳墠鍊?
            BigDecimal currentValue = getCurrentValue(rule, inventoryData);
            if (currentValue == null) continue;

            // 妫€鏌ユ槸鍚﹁Е鍙戦璀?
            if (evaluateCondition(rule, currentValue)) {
                // 妫€鏌ユ槸鍚﹀凡鏈夋湭瑙ｅ喅鐨勫悓绫诲瀷棰勮锛堥槻閲嶅锛?
                List<AlertRecord> existing = recordMapper.selectActiveByType(rule.getAlertType());
                boolean duplicate =
                        existing.stream()
                                .anyMatch(
                                        r ->
                                                r.getRuleCode().equals(rule.getRuleCode())
                                                        && Objects.equals(
                                                                r.getSkuCode(), rule.getSkuCode())
                                                        && Objects.equals(
                                                                r.getWarehouseCode(),
                                                                rule.getWarehouseCode()));

                if (!duplicate) {
                    AlertRecord record = createAlertRecord(rule, currentValue);
                    triggeredAlerts.add(record);

                    // 鑷姩澶勭悊
                    if ("Y".equals(rule.getAutoHandle())) {
                        autoHandleAlert(record, rule);
                    }

                    // 鍙戦€侀€氱煡
                }
            }
        }

        log.info(
                "棰勮妫€鏌ュ畬鎴? frequency={}, rules={}, triggered={}",
                frequency,
                rules.size(),
                triggeredAlerts.size());
        return triggeredAlerts;
    }

    /** 璇勪及棰勮鏉′欢 */
    private boolean evaluateCondition(AlertRule rule, BigDecimal currentValue) {
        BigDecimal threshold = rule.getThresholdValue();
        if (threshold == null) return false;

        switch (rule.getConditionType()) {
            case "LT":
                return currentValue.compareTo(threshold) < 0;
            case "GT":
                return currentValue.compareTo(threshold) > 0;
            case "EQ":
                return currentValue.compareTo(threshold) == 0;
            case "LTE":
                return currentValue.compareTo(threshold) <= 0;
            case "GTE":
                return currentValue.compareTo(threshold) >= 0;
            case "BETWEEN":
                BigDecimal threshold2 = rule.getThresholdValue2();
                if (threshold2 == null) return false;
                return currentValue.compareTo(threshold) >= 0
                        && currentValue.compareTo(threshold2) <= 0;
            default:
                return false;
        }
    }

    /** 鑾峰彇褰撳墠鍊? */
    private BigDecimal getCurrentValue(AlertRule rule, Map<String, Object> inventoryData) {
        // 浠庡簱瀛樻暟鎹腑鑾峰彇褰撳墠鍊?
        String key = rule.getAlertType() + "_" + rule.getSkuCode();
        Object value = inventoryData.get(key);
        if (value == null) value = inventoryData.get(rule.getAlertType());
        if (value == null) return null;
        return new BigDecimal(value.toString());
    }

    /** 鍒涘缓棰勮璁板綍 */
    private AlertRecord createAlertRecord(AlertRule rule, BigDecimal currentValue) {
        AlertRecord record = new AlertRecord();
        record.setAlertId(generateAlertId());
        record.setRuleCode(rule.getRuleCode());
        record.setAlertType(rule.getAlertType());
        record.setAlertLevel(rule.getAlertLevel());
        record.setWarehouseCode(rule.getWarehouseCode());
        record.setOwnerCode(rule.getOwnerCode());
        record.setSkuCode(rule.getSkuCode());
        record.setCurrentValue(currentValue);
        record.setThresholdValue(rule.getThresholdValue());

        // 璁＄畻鍋忓樊
        BigDecimal deviation = currentValue.subtract(rule.getThresholdValue());
        record.setDeviationValue(deviation);
        if (rule.getThresholdValue().compareTo(BigDecimal.ZERO) != 0) {
            record.setDeviationRate(
                    deviation
                            .divide(rule.getThresholdValue(), 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")));
        }

        // 鐢熸垚棰勮鏍囬鍜屽唴瀹?
        record.setAlertTitle(generateAlertTitle(rule, currentValue));
        record.setAlertContent(generateAlertContent(rule, currentValue, deviation));

        record.setStatus("PENDING");
        record.setTriggerTime(LocalDateTime.now());
        recordMapper.insert(record);

        log.info(
                "瑙﹀彂棰勮: alertId={}, type={}, level={}, current={}, threshold={}",
                record.getAlertId(),
                rule.getAlertType(),
                rule.getAlertLevel(),
                currentValue,
                rule.getThresholdValue());
        return record;
    }

    private String generateAlertTitle(AlertRule rule, BigDecimal currentValue) {
        return String.format(
                "[%s] %s - 褰撳墠鍊? %s, 闃堝€? %s",
                rule.getAlertLevel(), rule.getRuleName(), currentValue, rule.getThresholdValue());
    }

    private String generateAlertContent(
            AlertRule rule, BigDecimal currentValue, BigDecimal deviation) {
        return String.format(
                "棰勮瑙勫垯: %s\n棰勮绫诲瀷: %s\n浠撳簱: %s\nSKU: %s\n褰撳墠鍊? %s\n闃堝€? %s\n鍋忓樊: %s\n鍋忓樊鐜? %s%%",
                rule.getRuleName(),
                rule.getAlertType(),
                rule.getWarehouseCode(),
                rule.getSkuCode(),
                currentValue,
                rule.getThresholdValue(),
                deviation,
                rule.getThresholdValue().compareTo(BigDecimal.ZERO) != 0
                        ? deviation
                                .divide(rule.getThresholdValue(), 2, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO);
    }

    // ============================================================

    // 3. 棰勮澶勭悊
    // ============================================================

    /** 鎵嬪姩澶勭悊棰勮 */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord handleAlert(String alertId, String action, String note, String operator) {
        AlertRecord record = recordMapper.selectByAlertId(alertId);
        if (record == null) throw new RuntimeException("棰勮璁板綍涓嶅瓨鍦? " + alertId);

        String beforeStatus = record.getStatus();
        String afterStatus = "PROCESSING".equals(action) ? "PROCESSING" : "RESOLVED";

        // 鏇存柊棰勮鐘舵€?
        recordMapper.updateStatus(alertId, afterStatus, operator, note);

        // 璁板綍澶勭悊鏃ュ織
        AlertHandle handle = new AlertHandle();
        handle.setHandleId(generateHandleId());
        handle.setAlertId(alertId);
        handle.setHandleType("MANUAL");
        handle.setHandleAction(action);
        handle.setHandleNote(note);
        handle.setBeforeStatus(beforeStatus);
        handle.setAfterStatus(afterStatus);
        handle.setOperator(operator);
        handle.setHandleTime(LocalDateTime.now());
        handleMapper.insert(handle);

        log.info("澶勭悊棰勮: alertId={}, action={}, operator={}", alertId, action, operator);
        return recordMapper.selectByAlertId(alertId);
    }

    /** 鑷姩澶勭悊棰勮 */
    private void autoHandleAlert(AlertRecord record, AlertRule rule) {
        String action = rule.getHandleAction();
        if (action == null) return;

        AlertHandle handle = new AlertHandle();
        handle.setHandleId(generateHandleId());
        handle.setAlertId(record.getAlertId());
        handle.setHandleType("AUTO");
        handle.setHandleAction(action);
        handle.setHandleNote("绯荤粺鑷姩澶勭悊");
        handle.setBeforeStatus("PENDING");
        handle.setAfterStatus("PROCESSING");
        handle.setOperator("SYSTEM");
        handle.setHandleTime(LocalDateTime.now());
        handleMapper.insert(handle);

        recordMapper.updateStatus(record.getAlertId(), "PROCESSING", "SYSTEM", "绯荤粺鑷姩澶勭悊");
        log.info("鑷姩澶勭悊棰勮: alertId={}, action={}", record.getAlertId(), action);
    }

    /** 蹇界暐棰勮 */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord ignoreAlert(String alertId, String reason, String operator) {
        AlertRecord record = recordMapper.selectByAlertId(alertId);
        if (record == null) throw new RuntimeException("棰勮璁板綍涓嶅瓨鍦? " + alertId);

        String beforeStatus = record.getStatus();
        recordMapper.updateStatus(alertId, "IGNORED", operator, reason);

        AlertHandle handle = new AlertHandle();
        handle.setHandleId(generateHandleId());
        handle.setAlertId(alertId);
        handle.setHandleType("MANUAL");
        handle.setHandleAction("IGNORE");
        handle.setHandleNote(reason);
        handle.setBeforeStatus(beforeStatus);
        handle.setAfterStatus("IGNORED");
        handle.setOperator(operator);
        handle.setHandleTime(LocalDateTime.now());
        handleMapper.insert(handle);

        log.info("蹇界暐棰勮: alertId={}, reason={}", alertId, reason);
        return recordMapper.selectByAlertId(alertId);
    }

    /** 鍗囩骇棰勮 */
    @Transactional(rollbackFor = Exception.class)
    public AlertRecord escalateAlert(String alertId, String reason, String operator) {
        AlertRecord record = recordMapper.selectByAlertId(alertId);
        if (record == null) throw new RuntimeException("棰勮璁板綍涓嶅瓨鍦? " + alertId);

        AlertHandle handle = new AlertHandle();
        handle.setHandleId(generateHandleId());
        handle.setAlertId(alertId);
        handle.setHandleType("ESCALATE");
        handle.setHandleAction("ESCALATE");
        handle.setHandleNote(reason);
        handle.setBeforeStatus(record.getStatus());
        handle.setAfterStatus(record.getStatus());
        handle.setOperator(operator);
        handle.setHandleTime(LocalDateTime.now());
        handleMapper.insert(handle);

        log.info("鍗囩骇棰勮: alertId={}, reason={}", alertId, reason);
        return recordMapper.selectByAlertId(alertId);
    }

    // ============================================================

    // 4. 棰勮閫氱煡

    // ============================================================

    // 5. 棰勮鏌ヨ
    // ============================================================

    public AlertRecord getAlertById(String alertId) {
        return recordMapper.selectByAlertId(alertId);
    }

    public List<AlertRecord> getAlertsByStatus(String status) {
        return recordMapper.selectByStatus(status);
    }

    public List<AlertRecord> getActiveAlertsByType(String alertType) {
        return recordMapper.selectActiveByType(alertType);
    }

    public List<AlertRecord> getRecentAlertsBySku(String skuCode, int limit) {
        return recordMapper.selectRecentBySku(skuCode, limit);
    }

    public int countActiveAlerts() {
        return recordMapper.countActive();
    }

    public int countActiveAlertsByLevel(String level) {
        return recordMapper.countActiveByLevel(level);
    }

    public Page<AlertRecord> pageAlerts(
            Page<AlertRecord> page,
            String alertType,
            String status,
            String warehouseCode,
            String skuCode) {
        LambdaQueryWrapper<AlertRecord> wrapper = new LambdaQueryWrapper<>();
        if (alertType != null) wrapper.eq(AlertRecord::getAlertType, alertType);
        if (status != null) wrapper.eq(AlertRecord::getStatus, status);
        if (warehouseCode != null) wrapper.eq(AlertRecord::getWarehouseCode, warehouseCode);
        if (skuCode != null) wrapper.eq(AlertRecord::getSkuCode, skuCode);
        wrapper.orderByDesc(AlertRecord::getTriggerTime);
        return recordMapper.selectPage(page, wrapper);
    }

    public List<AlertHandle> getHandlesByAlert(String alertId) {
        return handleMapper.selectByAlertId(alertId);
    }

    // ============================================================

    // 6. 棰勮缁熻
    // ============================================================

    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActive", recordMapper.countActive());
        stats.put("infoActive", recordMapper.countActiveByLevel("INFO"));
        stats.put("warningActive", recordMapper.countActiveByLevel("WARNING"));
        stats.put("criticalActive", recordMapper.countActiveByLevel("CRITICAL"));
        stats.put("totalRules", ruleMapper.selectCount(null));
        return stats;
    }

    // ============================================================

    // 宸ュ叿鏂规硶
    // ============================================================

    private String generateAlertId() {
        return "ALT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateHandleId() {
        return "AHD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
