package com.xwms.core.serial.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.serial.entity.SerialNumber;
import com.xwms.core.serial.entity.SerialRecord;
import com.xwms.core.serial.entity.SerialRule;
import com.xwms.core.serial.service.SerialNumberService;

import lombok.RequiredArgsConstructor;

/** 序列号管理Controller */
@RestController
@RequestMapping("/api/serial")
@RequiredArgsConstructor
public class SerialNumberController {

    private final SerialNumberService serialNumberService;

    // ==================== 序列号采集 ====================

    /** 入库序列号采集 */
    @PostMapping("/inbound/collect")
    public SerialRecord collectInbound(
            @RequestParam String inboundNo,
            @RequestParam(required = false) String asnNo,
            @RequestParam String skuCode,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String batchNo,
            @RequestBody List<String> serialNos,
            @RequestParam(required = false, defaultValue = "system") String collector) {
        return serialNumberService.collectInboundSerials(
                inboundNo, asnNo, skuCode, ownerCode, warehouseCode, batchNo, serialNos, collector);
    }

    /** 出库序列号采集 */
    @PostMapping("/outbound/collect")
    public SerialRecord collectOutbound(
            @RequestParam String outboundNo,
            @RequestParam(required = false) String waveNo,
            @RequestParam String skuCode,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestBody List<String> serialNos,
            @RequestParam(required = false, defaultValue = "system") String collector) {
        return serialNumberService.collectOutboundSerials(
                outboundNo, waveNo, skuCode, ownerCode, warehouseCode, serialNos, collector);
    }

    /** 退货序列号采集 */
    @PostMapping("/return/collect")
    public SerialRecord collectReturn(
            @RequestParam String returnNo,
            @RequestParam String skuCode,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String batchNo,
            @RequestBody List<String> serialNos,
            @RequestParam(required = false, defaultValue = "system") String collector) {
        return serialNumberService.collectReturnSerials(
                returnNo, skuCode, ownerCode, warehouseCode, batchNo, serialNos, collector);
    }

    // ==================== 序列号校验 ====================

    /** 校验序列号 */
    @PostMapping("/validate")
    public Map<String, Object> validate(
            @RequestParam String skuCode,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String asnNo,
            @RequestBody List<String> serialNos) {
        serialNumberService.validateSerials(serialNos, skuCode, ownerCode, warehouseCode, asnNo);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "序列号校验通过");
        result.put("count", serialNos.size());
        return result;
    }

    // ==================== 2级序列号管理 ====================

    /** 创建箱级序列号 */
    @PostMapping("/box/create")
    public SerialNumber createBox(
            @RequestParam String boxSerialNo,
            @RequestParam String skuCode,
            @RequestParam String ownerCode,
            @RequestParam String warehouseCode,
            @RequestParam(required = false) String inboundNo,
            @RequestParam(required = false) String asnNo,
            @RequestBody List<String> childSerialNos,
            @RequestParam(required = false, defaultValue = "system") String creator) {
        return serialNumberService.createBoxSerial(
                boxSerialNo,
                skuCode,
                ownerCode,
                warehouseCode,
                inboundNo,
                asnNo,
                childSerialNos,
                creator);
    }

    /** 拆箱 */
    @PostMapping("/box/unpack")
    public Map<String, Object> unpackBox(
            @RequestParam String boxSerialNo,
            @RequestParam(required = false, defaultValue = "system") String operator) {
        serialNumberService.unpackBox(boxSerialNo, operator);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "拆箱成功");
        return result;
    }

    // ==================== 查询方法 ====================

    /** 根据序列号查询详情 */
    @GetMapping("/{serialNo}")
    public SerialNumber getBySerialNo(@PathVariable String serialNo) {
        return serialNumberService.getBySerialNo(serialNo);
    }

    /** 查询商品在库序列号 */
    @GetMapping("/in-stock/{skuCode}")
    public List<SerialNumber> getInStockBySku(
            @PathVariable String skuCode, @RequestParam String warehouseCode) {
        return serialNumberService.getInStockBySku(skuCode, warehouseCode);
    }

    /** 根据入库单查询序列号 */
    @GetMapping("/inbound/{inboundNo}")
    public List<SerialNumber> getByInboundNo(@PathVariable String inboundNo) {
        return serialNumberService.getByInboundNo(inboundNo);
    }

    /** 根据出库单查询序列号 */
    @GetMapping("/outbound/{outboundNo}")
    public List<SerialNumber> getByOutboundNo(@PathVariable String outboundNo) {
        return serialNumberService.getByOutboundNo(outboundNo);
    }

    /** 根据箱号查询子序列号 */
    @GetMapping("/box/{boxSerialNo}/children")
    public List<SerialNumber> getByParentSerialNo(@PathVariable String boxSerialNo) {
        return serialNumberService.getByParentSerialNo(boxSerialNo);
    }

    /** 查询所有序列号规则 */
    @GetMapping("/rules")
    public List<SerialRule> getAllRules() {
        return serialNumberService.getAllRules();
    }

    /** 查询采集记录 */
    @GetMapping("/records/{refNo}")
    public List<SerialRecord> getRecordsByRefNo(@PathVariable String refNo) {
        return serialNumberService.getRecordsByRefNo(refNo);
    }

    /** 检查是否开启序列号管理 */
    @GetMapping("/enabled")
    public Map<String, Object> isEnabled() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", serialNumberService.isSerialEnabled());
        return result;
    }
}
