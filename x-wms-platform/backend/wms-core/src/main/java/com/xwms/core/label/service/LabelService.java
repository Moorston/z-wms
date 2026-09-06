package com.xwms.core.label.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.label.entity.*;
import com.xwms.core.label.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 鏍囩鎵撳嵃涓庢潯鐮佺鐞嗘牳蹇冩湇鍔? 鍖呭惈: 鏉＄爜瑙勫垯/鏉＄爜鐢熸垚/鏍囩妯℃澘/鎵撳嵃浠诲姟 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {

    private final BarcodeRuleMapper barcodeRuleMapper;
    private final LabelTemplateMapper labelTemplateMapper;
    private final BarcodeRecordMapper barcodeRecordMapper;

    private static final AtomicInteger TASK_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    // ============================================================

    // 1. 鏉＄爜瑙勫垯绠＄悊
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public BarcodeRule createBarcodeRule(BarcodeRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getSequenceCurrent() == null) {
            rule.setSequenceCurrent(rule.getSequenceStart() != null ? rule.getSequenceStart() : 1L);
        }
        barcodeRuleMapper.insert(rule);
        log.info("鍒涘缓鏉＄爜瑙勫垯: {}", rule.getRuleCode());
        return rule;
    }

    public BarcodeRule getBarcodeRule(Long id) {
        BarcodeRule rule = barcodeRuleMapper.selectById(id);
        if (rule == null) throw new BizException("鏉＄爜瑙勫垯涓嶅瓨鍦? " + id);
        return rule;
    }

    public Page<BarcodeRule> pageBarcodeRules(Page<BarcodeRule> page, String type) {
        LambdaQueryWrapper<BarcodeRule> wrapper = new LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(BarcodeRule::getBarcodeType, type);
        wrapper.orderByDesc(BarcodeRule::getCreatedTime);
        return barcodeRuleMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 鏉＄爜鐢熸垚 (鏍稿績鏂规硶)
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public String generateBarcode(
            String barcodeType,
            String businessType,
            String businessNo,
            String sku,
            String batchNo,
            java.math.BigDecimal quantity,
            String generatedBy) {
        // 1. 鑾峰彇鏉＄爜瑙勫垯
        List<BarcodeRule> ruleList = barcodeRuleMapper.selectByBarcodeType(barcodeType);
        BarcodeRule rule = (ruleList != null && !ruleList.isEmpty()) ? ruleList.get(0) : null;
        if (rule == null) {
            throw new BizException("鏈壘鍒版潯鐮佺被鍨嬬殑瑙勫垯: " + barcodeType);
        }

        // 2. 鐢熸垚搴忓垪鍙?鍘熷瓙閫掑)
        barcodeRuleMapper.incrementSequence(rule.getRuleCode());
        BarcodeRule updated = barcodeRuleMapper.selectById(rule.getId());
        long seq = updated.getSequenceCurrent();

        // 3. 鎷兼帴鏉＄爜
        StringBuilder sb = new StringBuilder();
        if (rule.getPrefix() != null) sb.append(rule.getPrefix());
        if (rule.getDateFormat() != null && !rule.getDateFormat().isEmpty()) {
            sb.append(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(rule.getDateFormat())));
        }
        sb.append(String.format("%0" + rule.getSequenceLength() + "d", seq));
        if (rule.getSuffix() != null) sb.append(rule.getSuffix());

        // 4. 鏍￠獙浣?
        String barcode = sb.toString();
        if (rule.getChecksumType() != null && !"NONE".equals(rule.getChecksumType())) {
            barcode = barcode + calculateCheckDigit(barcode);
        }

        // 5. 淇濆瓨鏉＄爜璁板綍
        BarcodeRecord record = new BarcodeRecord();
        record.setBarcode(barcode);
        record.setBarcodeType(barcodeType);
        record.setRuleCode(rule.getRuleCode());
        record.setBusinessType(businessType);
        record.setBusinessNo(businessNo);
        record.setSku(sku);
        record.setBatchNo(batchNo);
        record.setQuantity(quantity);
        record.setStatus("ACTIVE");
        record.setGeneratedBy(generatedBy);
        record.setOwnerCode(rule.getOwnerCode());
        record.setWarehouseCode(rule.getWarehouseCode());
        barcodeRecordMapper.insert(record);

        log.info("鐢熸垚鏉＄爜: {}, 绫诲瀷={}, 瑙勫垯={}", barcode, barcodeType, rule.getRuleCode());
        return barcode;
    }

    /** 璁＄畻鏍￠獙浣?绠€鍗曠殑鍔犳潈鍙栨ā) */
    private String calculateCheckDigit(String code) {
        int sum = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = code.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit * 3 : digit;
        }
        int check = (10 - (sum % 10)) % 10;
        return String.valueOf(check);
    }

    public BarcodeRecord getBarcodeRecord(String barcode) {
        return barcodeRecordMapper.selectByBarcode(barcode);
    }

    @Transactional(rollbackFor = Exception.class)
    public void useBarcode(String barcode) {
        BarcodeRecord record = barcodeRecordMapper.selectByBarcode(barcode);
        if (record == null) throw new BizException("鏉＄爜涓嶅瓨鍦? " + barcode);
        if (!"ACTIVE".equals(record.getStatus())) {
            throw new BizException("鏉＄爜鐘舵€佷笉鍏佽浣跨敤: " + record.getStatus());
        }
        record.setStatus("USED");
        record.setUpdatedTime(LocalDateTime.now());
        barcodeRecordMapper.updateById(record);
    }

    // ============================================================

    // 3. 鏍囩妯℃澘绠＄悊
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public LabelTemplate createLabelTemplate(LabelTemplate template) {
        template.setStatus("ACTIVE");
        labelTemplateMapper.insert(template);
        log.info("鍒涘缓鏍囩妯℃澘: {}", template.getTemplateCode());
        return template;
    }

    public LabelTemplate getLabelTemplate(Long id) {
        LabelTemplate tpl = labelTemplateMapper.selectById(id);
        if (tpl == null) throw new BizException("鏍囩妯℃澘涓嶅瓨鍦? " + id);
        return tpl;
    }

    public LabelTemplate getLabelTemplateByCode(String code) {
        return labelTemplateMapper.selectByTemplateCode(code);
    }

    public Page<LabelTemplate> pageLabelTemplates(Page<LabelTemplate> page, String type) {
        LambdaQueryWrapper<LabelTemplate> wrapper = new LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(LabelTemplate::getTemplateType, type);
        wrapper.orderByDesc(LabelTemplate::getCreatedTime);
        return labelTemplateMapper.selectPage(page, wrapper);
    }

    /** 娓叉煋鏍囩妯℃澘 */
    public String renderLabel(String templateCode, Map<String, Object> variables) {
        LabelTemplate template = labelTemplateMapper.selectByTemplateCode(templateCode);
        if (template == null) throw new BizException("鏍囩妯℃澘涓嶅瓨鍦? " + templateCode);
        return renderTemplate(template.getTemplateContent(), variables);
    }

    private String renderTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) return template;
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            matcher.appendReplacement(
                    sb, value != null ? Matcher.quoteReplacement(value.toString()) : "");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
