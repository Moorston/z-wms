package com.xwms.core.serial.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.config.service.SysConfigService;
import com.xwms.core.serial.entity.SerialNumber;
import com.xwms.core.serial.entity.SerialRecord;
import com.xwms.core.serial.entity.SerialRule;
import com.xwms.core.serial.enums.SerialStatus;
import com.xwms.core.serial.mapper.SerialNumberMapper;
import com.xwms.core.serial.mapper.SerialRecordMapper;
import com.xwms.core.serial.mapper.SerialRuleMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 序列号管理服务 支持1级（单品级）和2级（箱号+单品）序列号管理 包含序列号采集、规则校验、重复性检查、入库/出库/退货处理 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SerialNumberService {

    private final SerialNumberMapper serialNumberMapper;
    private final SerialRuleMapper serialRuleMapper;
    private final SerialRecordMapper serialRecordMapper;
    private final SysConfigService sysConfigService;

    // ==================== 序列号采集 ====================

    /**
     * 入库序列号采集
     *
     * @param inboundNo 入库单号
     * @param asnNo ASN号
     * @param skuCode 商品编码
     * @param ownerCode 货主编码
     * @param warehouseCode 仓库编码
     * @param batchNo 批次号
     * @param serialNos 序列号列表
     * @param collector 采集人
     */
    @Transactional(rollbackFor = Exception.class)
    public SerialRecord collectInboundSerials(
            String inboundNo,
            String asnNo,
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            List<String> serialNos,
            String collector) {
        log.info("入库序列号采集: inboundNo={}, skuCode={}, 数量={}", inboundNo, skuCode, serialNos.size());

        // 1. 获取序列号管理模式
        Integer serialMode = sysConfigService.getIntConfig(SysConfigService.SN_CTL);
        if (serialMode == null || serialMode == 0) {
            throw new RuntimeException("当前未开启序列号管理（SN#_CTL=0）");
        }

        // 2. 校验序列号
        validateSerials(serialNos, skuCode, ownerCode, warehouseCode, asnNo);

        // 3. 创建采集记录
        SerialRecord record = new SerialRecord();
        record.setRecordNo(generateRecordNo("SR"));
        record.setBusinessType("INBOUND");
        record.setRefNo(inboundNo);
        record.setAsnNo(asnNo);
        record.setSkuCode(skuCode);
        record.setOwnerCode(ownerCode);
        record.setWarehouseCode(warehouseCode);
        record.setBatchNo(batchNo);
        record.setSerialNos(String.join(",", serialNos));
        record.setCollectQty(serialNos.size());
        record.setExpectedQty(serialNos.size());
        record.setStatus("COMPLETED");
        record.setCollectMode("SCAN");
        record.setCollector(collector);
        record.setStartTime(LocalDateTime.now());
        record.setFinishTime(LocalDateTime.now());
        record.setCreatedBy(collector);
        record.setCreatedTime(LocalDateTime.now());
        serialRecordMapper.insert(record);

        // 4. 保存序列号
        List<SerialNumber> numbers = new ArrayList<>();
        for (String serialNo : serialNos) {
            SerialNumber sn = new SerialNumber();
            sn.setSerialNo(serialNo);
            sn.setSerialLevel(1); // 单品级
            sn.setSkuCode(skuCode);
            sn.setOwnerCode(ownerCode);
            sn.setWarehouseCode(warehouseCode);
            sn.setBatchNo(batchNo);
            sn.setInboundNo(inboundNo);
            sn.setAsnNo(asnNo);
            sn.setStatus(SerialStatus.IN_STOCK.getCode());
            sn.setInboundTime(LocalDateTime.now());
            sn.setCreatedBy(collector);
            sn.setCreatedTime(LocalDateTime.now());
            numbers.add(sn);
        }
        serialNumberMapper.batchInsert(numbers);

        log.info("入库序列号采集完成: recordNo={}, 数量={}", record.getRecordNo(), serialNos.size());
        return record;
    }

    /** 出库序列号采集 */
    @Transactional(rollbackFor = Exception.class)
    public SerialRecord collectOutboundSerials(
            String outboundNo,
            String waveNo,
            String skuCode,
            String ownerCode,
            String warehouseCode,
            List<String> serialNos,
            String collector) {
        log.info(
                "出库序列号采集: outboundNo={}, skuCode={}, 数量={}", outboundNo, skuCode, serialNos.size());

        // 校验序列号是否在库
        for (String serialNo : serialNos) {
            SerialNumber sn = serialNumberMapper.selectBySerialNo(serialNo);
            if (sn == null) {
                throw new RuntimeException("序列号不存在: " + serialNo);
            }
            if (!SerialStatus.IN_STOCK.getCode().equals(sn.getStatus())) {
                throw new RuntimeException("序列号不在库: " + serialNo + ", 状态=" + sn.getStatus());
            }
            if (!skuCode.equals(sn.getSkuCode())) {
                throw new RuntimeException("序列号商品不匹配: " + serialNo);
            }
        }

        // 创建采集记录
        SerialRecord record = new SerialRecord();
        record.setRecordNo(generateRecordNo("SR"));
        record.setBusinessType("OUTBOUND");
        record.setRefNo(outboundNo);
        record.setSkuCode(skuCode);
        record.setOwnerCode(ownerCode);
        record.setWarehouseCode(warehouseCode);
        record.setSerialNos(String.join(",", serialNos));
        record.setCollectQty(serialNos.size());
        record.setExpectedQty(serialNos.size());
        record.setStatus("COMPLETED");
        record.setCollectMode("SCAN");
        record.setCollector(collector);
        record.setStartTime(LocalDateTime.now());
        record.setFinishTime(LocalDateTime.now());
        record.setCreatedBy(collector);
        record.setCreatedTime(LocalDateTime.now());
        serialRecordMapper.insert(record);

        // 更新序列号状态为已出库
        serialNumberMapper.batchUpdateStatus(serialNos, SerialStatus.OUT_STOCK.getCode());
        // 更新出库单号和出库时间
        for (String serialNo : serialNos) {
            SerialNumber sn = serialNumberMapper.selectBySerialNo(serialNo);
            if (sn != null) {
                sn.setOutboundNo(outboundNo);
                sn.setWaveNo(waveNo);
                sn.setOutboundTime(LocalDateTime.now());
                serialNumberMapper.updateById(sn);
            }
        }

        log.info("出库序列号采集完成: recordNo={}", record.getRecordNo());
        return record;
    }

    /** 退货序列号采集 */
    @Transactional(rollbackFor = Exception.class)
    public SerialRecord collectReturnSerials(
            String returnNo,
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String batchNo,
            List<String> serialNos,
            String collector) {
        log.info("退货序列号采集: returnNo={}, skuCode={}, 数量={}", returnNo, skuCode, serialNos.size());

        // 校验序列号是否已出库（退货必须是已出库的序列号）
        for (String serialNo : serialNos) {
            SerialNumber sn = serialNumberMapper.selectBySerialNo(serialNo);
            if (sn == null) {
                throw new RuntimeException("序列号不存在: " + serialNo);
            }
            if (!SerialStatus.OUT_STOCK.getCode().equals(sn.getStatus())) {
                throw new RuntimeException("序列号不是已出库状态，无法退货: " + serialNo);
            }
        }

        // 创建采集记录
        SerialRecord record = new SerialRecord();
        record.setRecordNo(generateRecordNo("SR"));
        record.setBusinessType("RETURN");
        record.setRefNo(returnNo);
        record.setSkuCode(skuCode);
        record.setOwnerCode(ownerCode);
        record.setWarehouseCode(warehouseCode);
        record.setBatchNo(batchNo);
        record.setSerialNos(String.join(",", serialNos));
        record.setCollectQty(serialNos.size());
        record.setExpectedQty(serialNos.size());
        record.setStatus("COMPLETED");
        record.setCollectMode("SCAN");
        record.setCollector(collector);
        record.setStartTime(LocalDateTime.now());
        record.setFinishTime(LocalDateTime.now());
        record.setCreatedBy(collector);
        record.setCreatedTime(LocalDateTime.now());
        serialRecordMapper.insert(record);

        // 更新序列号状态为退货（在库）
        serialNumberMapper.batchUpdateStatus(serialNos, SerialStatus.RETURNED.getCode());

        log.info("退货序列号采集完成: recordNo={}", record.getRecordNo());
        return record;
    }

    // ==================== 序列号校验 ====================

    /** 校验序列号（规则校验+重复性校验） */
    public void validateSerials(
            List<String> serialNos,
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String asnNo) {
        if (serialNos == null || serialNos.isEmpty()) {
            throw new RuntimeException("序列号列表不能为空");
        }

        // 1. 获取序列号规则
        SerialRule rule = serialRuleMapper.selectBySkuAndOwner(skuCode, ownerCode);

        // 2. 获取入库序列号验证方式
        Integer validateMode = sysConfigService.getIntConfig(SysConfigService.SN_RCV_VAL);
        if (validateMode == null) {
            validateMode = 1; // 默认当前仓库校验
        }

        Set<String> scannedSet = new HashSet<>();
        for (String serialNo : serialNos) {
            // 非空校验
            if (serialNo == null || serialNo.trim().isEmpty()) {
                throw new RuntimeException("序列号不能为空");
            }
            serialNo = serialNo.trim();

            // 规则校验
            if (rule != null) {
                validateByRule(serialNo, rule);
            }

            // 扫描队列内重复性校验
            if (scannedSet.contains(serialNo)) {
                throw new RuntimeException("序列号在扫描队列中重复: " + serialNo);
            }
            scannedSet.add(serialNo);

            // 系统重复性校验
            switch (validateMode) {
                case 1: // 当前仓库在库序列号
                    if (serialNumberMapper.countInStockInWarehouse(serialNo, warehouseCode) > 0) {
                        throw new RuntimeException("序列号已在当前仓库存在: " + serialNo);
                    }
                    break;
                case 2: // 当前ASN在库序列号
                    if (serialNumberMapper.countInAsn(serialNo, asnNo) > 0) {
                        throw new RuntimeException("序列号已在当前ASN中存在: " + serialNo);
                    }
                    break;
                case 3: // 扫描队列中不可重复（已在上面校验）
                    break;
                default:
                    // 全局校验
                    if (serialNumberMapper.countBySerialNo(serialNo) > 0) {
                        throw new RuntimeException("序列号已存在: " + serialNo);
                    }
            }
        }
    }

    /** 根据规则校验序列号 */
    private void validateByRule(String serialNo, SerialRule rule) {
        // 长度校验
        if (rule.getLength() != null && rule.getLength() > 0) {
            if (serialNo.length() != rule.getLength()) {
                throw new RuntimeException(
                        "序列号长度不符合规则: " + serialNo + ", 期望长度=" + rule.getLength());
            }
        }
        if (rule.getMinLength() != null && serialNo.length() < rule.getMinLength()) {
            throw new RuntimeException("序列号长度小于最小长度: " + serialNo);
        }
        if (rule.getMaxLength() != null && serialNo.length() > rule.getMaxLength()) {
            throw new RuntimeException("序列号长度大于最大长度: " + serialNo);
        }

        // 前缀校验
        if (rule.getPrefix() != null && !rule.getPrefix().isEmpty()) {
            String[] prefixes = rule.getPrefix().split(",");
            boolean match = false;
            for (String prefix : prefixes) {
                if (serialNo.startsWith(prefix.trim())) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                throw new RuntimeException("序列号前缀不符合规则: " + serialNo);
            }
        }

        // 后缀校验
        if (rule.getSuffix() != null && !rule.getSuffix().isEmpty()) {
            String[] suffixes = rule.getSuffix().split(",");
            boolean match = false;
            for (String suffix : suffixes) {
                if (serialNo.endsWith(suffix.trim())) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                throw new RuntimeException("序列号后缀不符合规则: " + serialNo);
            }
        }

        // 字符类型校验
        if (rule.getCharType() != null) {
            switch (rule.getCharType()) {
                case "ALPHA":
                    if (!Pattern.matches("^[a-zA-Z]+$", serialNo)) {
                        throw new RuntimeException("序列号必须为字母: " + serialNo);
                    }
                    break;
                case "NUMERIC":
                    if (!Pattern.matches("^[0-9]+$", serialNo)) {
                        throw new RuntimeException("序列号必须为数字: " + serialNo);
                    }
                    break;
                case "ALPHANUMERIC":
                    if (!Pattern.matches("^[a-zA-Z0-9]+$", serialNo)) {
                        throw new RuntimeException("序列号必须为字母数字: " + serialNo);
                    }
                    break;
                default:
                    break;
            }
        }

        // 医药行业强制长度校验
        if ("Y".equals(rule.getMedicalForceLength())) {
            if (serialNo.length() != 16 && serialNo.length() != 20) {
                throw new RuntimeException("医药行业序列号必须为16或20位: " + serialNo);
            }
        }
    }

    // ==================== 2级序列号管理（箱号+单品） ====================

    /** 创建箱级序列号（2级） */
    @Transactional(rollbackFor = Exception.class)
    public SerialNumber createBoxSerial(
            String boxSerialNo,
            String skuCode,
            String ownerCode,
            String warehouseCode,
            String inboundNo,
            String asnNo,
            List<String> childSerialNos,
            String creator) {
        log.info("创建箱级序列号: boxNo={}, 子序列号数量={}", boxSerialNo, childSerialNos.size());

        // 校验箱号是否存在
        if (serialNumberMapper.countBySerialNo(boxSerialNo) > 0) {
            throw new RuntimeException("箱号已存在: " + boxSerialNo);
        }

        // 创建箱级序列号
        SerialNumber box = new SerialNumber();
        box.setSerialNo(boxSerialNo);
        box.setSerialLevel(2); // 箱级
        box.setSkuCode(skuCode);
        box.setOwnerCode(ownerCode);
        box.setWarehouseCode(warehouseCode);
        box.setInboundNo(inboundNo);
        box.setAsnNo(asnNo);
        box.setStatus(SerialStatus.IN_STOCK.getCode());
        box.setInboundTime(LocalDateTime.now());
        box.setCreatedBy(creator);
        box.setCreatedTime(LocalDateTime.now());
        serialNumberMapper.insert(box);

        // 更新子序列号的父序列号
        for (String childNo : childSerialNos) {
            SerialNumber child = serialNumberMapper.selectBySerialNo(childNo);
            if (child != null) {
                child.setParentSerialNo(boxSerialNo);
                serialNumberMapper.updateById(child);
            }
        }

        log.info("箱级序列号创建完成: boxNo={}", boxSerialNo);
        return box;
    }

    /** 拆箱（解除箱号与子序列号关联） */
    @Transactional(rollbackFor = Exception.class)
    public void unpackBox(String boxSerialNo, String operator) {
        log.info("拆箱: boxNo={}", boxSerialNo);

        SerialNumber box = serialNumberMapper.selectBySerialNo(boxSerialNo);
        if (box == null || box.getSerialLevel() != 2) {
            throw new RuntimeException("箱号不存在或不是箱级序列号: " + boxSerialNo);
        }

        // 查询子序列号
        List<SerialNumber> children = serialNumberMapper.selectByParentSerialNo(boxSerialNo);
        for (SerialNumber child : children) {
            child.setParentSerialNo(null);
            serialNumberMapper.updateById(child);
        }

        // 删除箱号
        serialNumberMapper.deleteById(box.getId());

        log.info("拆箱完成: boxNo={}, 子序列号数量={}", boxSerialNo, children.size());
    }

    // ==================== 查询方法 ====================

    /** 根据序列号查询详情 */
    public SerialNumber getBySerialNo(String serialNo) {
        return serialNumberMapper.selectBySerialNo(serialNo);
    }

    /** 查询商品在库序列号列表 */
    public List<SerialNumber> getInStockBySku(String skuCode, String warehouseCode) {
        return serialNumberMapper.selectInStockBySku(skuCode, warehouseCode);
    }

    /** 根据入库单查询序列号 */
    public List<SerialNumber> getByInboundNo(String inboundNo) {
        return serialNumberMapper.selectByInboundNo(inboundNo);
    }

    /** 根据出库单查询序列号 */
    public List<SerialNumber> getByOutboundNo(String outboundNo) {
        return serialNumberMapper.selectByOutboundNo(outboundNo);
    }

    /** 根据箱号查询子序列号 */
    public List<SerialNumber> getByParentSerialNo(String parentSerialNo) {
        return serialNumberMapper.selectByParentSerialNo(parentSerialNo);
    }

    /** 查询所有序列号规则 */
    public List<SerialRule> getAllRules() {
        return serialRuleMapper.selectList(
                new LambdaQueryWrapper<SerialRule>().orderByDesc(SerialRule::getCreatedTime));
    }

    /** 查询采集记录 */
    public List<SerialRecord> getRecordsByRefNo(String refNo) {
        return serialRecordMapper.selectByRefNo(refNo);
    }

    // ==================== 工具方法 ====================

    /** 生成采集单号 */
    private String generateRecordNo(String prefix) {
        return prefix + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }

    /** 检查是否开启序列号管理 */
    public boolean isSerialEnabled() {
        Integer mode = sysConfigService.getIntConfig(SysConfigService.SN_CTL);
        return mode != null && mode > 0;
    }
}
