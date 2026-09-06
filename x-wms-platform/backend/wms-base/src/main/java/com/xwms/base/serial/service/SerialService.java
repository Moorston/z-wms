package com.xwms.base.serial.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.serial.entity.*;
import com.xwms.base.serial.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 序列号管理核心服务 核心能力: 序列号生成/绑定/状态流转/追踪 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerialService {

    private final SerialRuleMapper ruleMapper;
    private final SerialMapper serialMapper;
    private final SerialBindMapper bindMapper;
    private final SerialTraceMapper traceMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. 序列号规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public SerialRule createRule(SerialRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getSeqLength() == null) rule.setSeqLength(10);
        if (rule.getStartSeq() == null) rule.setStartSeq(1L);
        if (rule.getCurrentSeq() == null) rule.setCurrentSeq(1L);
        if (rule.getCheckDigit() == null) rule.setCheckDigit(0);
        ruleMapper.insert(rule);
        log.info("创建序列号规则: {}={}", rule.getRuleCode(), rule.getRuleName());
        return rule;
    }

    public Page<SerialRule> pageRules(Page<SerialRule> page, String skuCode) {
        LambdaQueryWrapper<SerialRule> wrapper = new LambdaQueryWrapper<>();
        if (skuCode != null) wrapper.eq(SerialRule::getSkuCode, skuCode);
        wrapper.eq(SerialRule::getStatus, "ACTIVE");
        return ruleMapper.selectPage(page, wrapper);
    }

    public SerialRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    // ============================================================
    // 2. 序列号生成
    // ============================================================

    /** 批量生成序列号 */
    @Transactional(rollbackFor = Exception.class)
    public List<Serial> generateSerials(
            String ruleCode,
            String skuCode,
            String batchNo,
            int count,
            String ownerCode,
            String warehouseCode) {
        SerialRule rule = ruleMapper.selectByRuleCode(ruleCode);
        if (rule == null) throw new RuntimeException("序列号规则不存在: " + ruleCode);

        List<Serial> serials = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            // 原子递增序号
            ruleMapper.incrementCurrentSeq(ruleCode);
            rule = ruleMapper.selectByRuleCode(ruleCode);

            String serialNo = buildSerialNo(rule, rule.getCurrentSeq());

            Serial serial = new Serial();
            serial.setSerialNo(serialNo);
            serial.setSkuCode(skuCode);
            serial.setBatchNo(batchNo);
            serial.setStatus("CREATED");
            serial.setOwnerCode(ownerCode);
            serial.setWarehouseCode(warehouseCode);
            serialMapper.insert(serial);

            // 记录流转
            recordTrace(
                    serialNo, "GENERATE", null, "CREATED", null, null, null, null, null, "系统生成");

            serials.add(serial);
        }

        log.info("批量生成序列号: rule={}, sku={}, count={}", ruleCode, skuCode, count);
        return serials;
    }

    /** 构建序列号 格式: 前缀 + 日期 + 序号 + 校验位 + 后缀 */
    private String buildSerialNo(SerialRule rule, Long seq) {
        StringBuilder sb = new StringBuilder();
        if (rule.getPrefix() != null) sb.append(rule.getPrefix());
        if (rule.getDateFormat() != null) {
            sb.append(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(rule.getDateFormat())));
        }
        String seqStr = String.format("%0" + rule.getSeqLength() + "d", seq);
        sb.append(seqStr);
        if (rule.getCheckDigit() != null && rule.getCheckDigit() == 1) {
            sb.append(calcCheckDigit(seqStr));
        }
        if (rule.getSuffix() != null) sb.append(rule.getSuffix());
        return sb.toString();
    }

    /** 计算校验位（Luhn算法简化版） */
    private int calcCheckDigit(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int digit = Character.getNumericValue(number.charAt(i));
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) digit -= 9;
            }
            sum += digit;
        }
        return (10 - sum % 10) % 10;
    }

    // ============================================================
    // 3. 序列号档案管理
    // ============================================================

    public Page<Serial> pageSerials(
            Page<Serial> page,
            String skuCode,
            String status,
            String batchNo,
            String locationCode,
            String warehouseCode) {
        LambdaQueryWrapper<Serial> wrapper = new LambdaQueryWrapper<>();
        if (skuCode != null) wrapper.eq(Serial::getSkuCode, skuCode);
        if (status != null) wrapper.eq(Serial::getStatus, status);
        if (batchNo != null) wrapper.eq(Serial::getBatchNo, batchNo);
        if (locationCode != null) wrapper.eq(Serial::getLocationCode, locationCode);
        if (warehouseCode != null) wrapper.eq(Serial::getWarehouseCode, warehouseCode);
        wrapper.orderByDesc(Serial::getCreatedTime);
        return serialMapper.selectPage(page, wrapper);
    }

    public Serial getSerialByNo(String serialNo) {
        return serialMapper.selectBySerialNo(serialNo);
    }

    public List<Serial> getSerialsByBatch(String batchNo) {
        return serialMapper.selectByBatchNo(batchNo);
    }

    public List<Serial> getSerialsByOutbound(String outboundNo) {
        return serialMapper.selectByOutbound(outboundNo);
    }

    public int countInStockBySku(String skuCode) {
        return serialMapper.countInStockBySku(skuCode);
    }

    // ============================================================
    // 4. 序列号状态流转
    // ============================================================

    /** 入库确认 */
    @Transactional(rollbackFor = Exception.class)
    public Serial inboundSerial(
            String serialNo,
            String inboundNo,
            String locationCode,
            String containerNo,
            String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        String fromStatus = serial.getStatus();
        serial.setStatus("IN_STOCK");
        serial.setInboundNo(inboundNo);
        serial.setLocationCode(locationCode);
        serial.setContainerNo(containerNo);
        serialMapper.updateById(serial);

        recordTrace(
                serialNo,
                "INBOUND",
                fromStatus,
                "IN_STOCK",
                null,
                locationCode,
                "INBOUND",
                inboundNo,
                operator,
                "入库确认");

        log.info("序列号入库: {}, inbound={}", serialNo, inboundNo);
        return serial;
    }

    /** 上架 */
    @Transactional(rollbackFor = Exception.class)
    public Serial putawaySerial(
            String serialNo, String fromLocation, String toLocation, String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        String fromStatus = serial.getStatus();
        serial.setLocationCode(toLocation);
        serialMapper.updateById(serial);

        recordTrace(
                serialNo,
                "PUTAWAY",
                fromStatus,
                fromStatus,
                fromLocation,
                toLocation,
                null,
                null,
                operator,
                "上架");

        log.info("序列号上架: {}, {}->{}", serialNo, fromLocation, toLocation);
        return serial;
    }

    /** 拣货 */
    @Transactional(rollbackFor = Exception.class)
    public Serial pickSerial(
            String serialNo, String outboundNo, String fromLocation, String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        String fromStatus = serial.getStatus();
        serial.setStatus("PICKED");
        serial.setOutboundNo(outboundNo);
        serialMapper.updateById(serial);

        recordTrace(
                serialNo,
                "PICK",
                fromStatus,
                "PICKED",
                fromLocation,
                null,
                "OUTBOUND",
                outboundNo,
                operator,
                "拣货");

        log.info("序列号拣货: {}, outbound={}", serialNo, outboundNo);
        return serial;
    }

    /** 打包 */
    @Transactional(rollbackFor = Exception.class)
    public Serial packSerial(String serialNo, String containerNo, String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        String fromStatus = serial.getStatus();
        serial.setStatus("PACKED");
        serial.setContainerNo(containerNo);
        serialMapper.updateById(serial);

        recordTrace(serialNo, "PACK", fromStatus, "PACKED", null, null, null, null, operator, "打包");

        log.info("序列号打包: {}, container={}", serialNo, containerNo);
        return serial;
    }

    /** 发运 */
    @Transactional(rollbackFor = Exception.class)
    public Serial shipSerial(String serialNo, String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        String fromStatus = serial.getStatus();
        serial.setStatus("SHIPPED");
        serialMapper.updateById(serial);

        recordTrace(
                serialNo, "SHIP", fromStatus, "SHIPPED", null, null, null, null, operator, "发运");

        log.info("序列号发运: {}", serialNo);
        return serial;
    }

    /** 退货 */
    @Transactional(rollbackFor = Exception.class)
    public Serial returnSerial(
            String serialNo, String returnNo, String locationCode, String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        String fromStatus = serial.getStatus();
        serial.setStatus("RETURNED");
        serial.setLocationCode(locationCode);
        serialMapper.updateById(serial);

        recordTrace(
                serialNo,
                "RETURN",
                fromStatus,
                "RETURNED",
                null,
                locationCode,
                "RETURN",
                returnNo,
                operator,
                "退货");

        log.info("序列号退货: {}, return={}", serialNo, returnNo);
        return serial;
    }

    /** 报废 */
    @Transactional(rollbackFor = Exception.class)
    public Serial scrapSerial(String serialNo, String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        String fromStatus = serial.getStatus();
        serial.setStatus("SCRAPPED");
        serialMapper.updateById(serial);

        recordTrace(
                serialNo, "SCRAP", fromStatus, "SCRAPPED", null, null, null, null, operator, "报废");

        log.info("序列号报废: {}", serialNo);
        return serial;
    }

    /** 移库 */
    @Transactional(rollbackFor = Exception.class)
    public Serial moveSerial(
            String serialNo, String fromLocation, String toLocation, String operator) {
        Serial serial = serialMapper.selectBySerialNo(serialNo);
        if (serial == null) throw new RuntimeException("序列号不存在: " + serialNo);

        serial.setLocationCode(toLocation);
        serialMapper.updateById(serial);

        recordTrace(
                serialNo,
                "MOVE",
                serial.getStatus(),
                serial.getStatus(),
                fromLocation,
                toLocation,
                null,
                null,
                operator,
                "移库");

        log.info("序列号移库: {}, {}->{}", serialNo, fromLocation, toLocation);
        return serial;
    }

    // ============================================================
    // 5. 序列号绑定
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public SerialBind bindSerial(
            String serialNo,
            String refType,
            String refNo,
            Integer refLineNo,
            String containerNo,
            String operator) {
        SerialBind bind = new SerialBind();
        bind.setBindNo(generateBindNo());
        bind.setSerialNo(serialNo);
        bind.setRefType(refType);
        bind.setRefNo(refNo);
        bind.setRefLineNo(refLineNo);
        bind.setContainerNo(containerNo);
        bind.setStatus("BOUND");
        bind.setBindTime(LocalDateTime.now());
        bind.setOperator(operator);
        bindMapper.insert(bind);

        log.info("序列号绑定: {}, ref={}/{}", serialNo, refType, refNo);
        return bind;
    }

    @Transactional(rollbackFor = Exception.class)
    public SerialBind releaseSerial(
            String serialNo, String refType, String refNo, String operator) {
        List<SerialBind> binds = bindMapper.selectActiveBySerial(serialNo);
        SerialBind target = null;
        for (SerialBind bind : binds) {
            if (refType.equals(bind.getRefType()) && refNo.equals(bind.getRefNo())) {
                target = bind;
                break;
            }
        }
        if (target == null) {
            throw new RuntimeException("未找到绑定记录: " + serialNo + "/" + refType + "/" + refNo);
        }
        target.setStatus("RELEASED");
        target.setReleaseTime(LocalDateTime.now());
        bindMapper.updateById(target);

        log.info("序列号释放: {}, ref={}/{}", serialNo, refType, refNo);
        return target;
    }

    public List<SerialBind> getSerialBinds(String serialNo) {
        return bindMapper.selectActiveBySerial(serialNo);
    }

    public List<SerialBind> getBindsByRef(String refType, String refNo) {
        return bindMapper.selectByRef(refType, refNo);
    }

    // ============================================================
    // 6. 序列号追踪
    // ============================================================

    public List<SerialTrace> getSerialTrace(String serialNo) {
        return traceMapper.selectBySerialNo(serialNo);
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private void recordTrace(
            String serialNo,
            String actionType,
            String fromStatus,
            String toStatus,
            String fromLocation,
            String toLocation,
            String refType,
            String refNo,
            String operator,
            String remark) {
        SerialTrace trace = new SerialTrace();
        trace.setTraceNo(generateTraceNo());
        trace.setSerialNo(serialNo);
        trace.setActionType(actionType);
        trace.setFromStatus(fromStatus);
        trace.setToStatus(toStatus);
        trace.setFromLocation(fromLocation);
        trace.setToLocation(toLocation);
        trace.setRefType(refType);
        trace.setRefNo(refNo);
        trace.setOperator(operator);
        trace.setActionTime(LocalDateTime.now());
        trace.setRemark(remark);
        traceMapper.insert(trace);
    }

    private String generateBindNo() {
        return "SB"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTraceNo() {
        return "ST"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
