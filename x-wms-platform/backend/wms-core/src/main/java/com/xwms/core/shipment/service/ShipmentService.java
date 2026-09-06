package com.xwms.core.shipment.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.shipment.entity.*;
import com.xwms.core.shipment.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 发运管理核心服务 核心能力: 发运单创建/承运商选择/快递单获取/批量打印/发运确认 高并发场景: 批量获取快递单和批量打印，使用异步处理+重试机制 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentMapper shipmentMapper;
    private final ShipmentDetailMapper detailMapper;
    private final ExpressOrderMapper expressOrderMapper;
    private final ShipmentTaskMapper taskMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final int MAX_RETRY = 3;

    // ============================================================

    // 1. 发运单创建
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Shipment createShipment(
            Shipment shipment, List<ShipmentDetail> details, String operator) {
        if (shipment.getShipmentNo() == null) {
            shipment.setShipmentNo(generateShipmentNo());
        }
        if (shipment.getStatus() == null) shipment.setStatus("CREATED");
        if (shipment.getTotalQty() == null) shipment.setTotalQty(BigDecimal.ZERO);
        if (shipment.getShippedQty() == null) shipment.setShippedQty(BigDecimal.ZERO);
        if (shipment.getPackageCount() == null) shipment.setPackageCount(0);
        if (shipment.getTotalWeight() == null) shipment.setTotalWeight(BigDecimal.ZERO);
        if (shipment.getTotalVolume() == null) shipment.setTotalVolume(BigDecimal.ZERO);
        if (shipment.getShippingFee() == null) shipment.setShippingFee(BigDecimal.ZERO);
        shipment.setCreatedBy(operator);
        shipmentMapper.insert(shipment);

        // 保存明细
        if (details != null) {
            for (int i = 0; i < details.size(); i++) {
                ShipmentDetail detail = details.get(i);
                detail.setShipmentNo(shipment.getShipmentNo());
                detail.setLineNo(i + 1);
                if (detail.getShippedQty() == null) detail.setShippedQty(BigDecimal.ZERO);
                if (detail.getStatus() == null) detail.setStatus("PENDING");
                detailMapper.insert(detail);
            }
            shipment.setTotalQty(
                    details.stream()
                            .map(ShipmentDetail::getShippedQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            shipmentMapper.updateById(shipment);
        }

        log.info(
                "创建发运单: {}={}, 明细{}行",
                shipment.getShipmentNo(),
                shipment.getCarrier(),
                details != null ? details.size() : 0);
        return shipment;
    }

    // ============================================================

    // 2. 承运商选择（配送规则）
    // ============================================================

    /** 根据配送规则选择承运商 TODO: 调用配送规则引擎，根据地址/重量/时效/成本选择承运商 */
    public String selectCarrier(
            String receiverAddress, BigDecimal weight, String serviceType, String ownerCode) {
        // 简化处理，实际应调用规则引擎
        log.info("选择承运商: address={}, weight={}, service={}", receiverAddress, weight, serviceType);
        return "SF"; // 默认顺丰
    }

    // ============================================================

    // 3. 快递单获取（高并发场景，异步处理）
    // ============================================================

    /** 获取单个快递单号 TODO: 调用承运商API获取运单号，使用Resilience4j限流降级 */
    @Transactional(rollbackFor = Exception.class)
    public ExpressOrder getTrackingNo(
            String shipmentNo,
            String carrier,
            String serviceType,
            String senderName,
            String senderPhone,
            String senderAddress,
            String receiverName,
            String receiverPhone,
            String receiverAddress,
            BigDecimal weight,
            BigDecimal volume) {
        Shipment shipment = shipmentMapper.selectByShipmentNo(shipmentNo);
        if (shipment == null) throw new RuntimeException("发运单不存在: " + shipmentNo);

        // 创建快递单
        ExpressOrder express = new ExpressOrder();
        express.setExpressNo(generateExpressNo());
        express.setShipmentNo(shipmentNo);
        express.setOutboundNo(shipment.getOutboundNo());
        express.setCarrier(carrier);
        express.setServiceType(serviceType);
        express.setStatus("CREATED");
        express.setSenderName(senderName);
        express.setSenderPhone(senderPhone);
        express.setSenderAddress(senderAddress);
        express.setReceiverName(receiverName);
        express.setReceiverPhone(receiverPhone);
        express.setReceiverAddress(receiverAddress);
        express.setWeight(weight);
        express.setVolume(volume);
        express.setRetryCount(0);
        expressOrderMapper.insert(express);

        // TODO: 调用承运商API获取运单号（异步处理，使用Kafka或线程池）
        // 这里简化处理，直接生成模拟运单号
        String trackingNo = carrier + LocalDateTime.now().format(NO_FMT) + SEQ.incrementAndGet();
        express.setTrackingNo(trackingNo);
        express.setStatus("CREATED");
        expressOrderMapper.updateById(express);

        // 创建作业记录
        createTask(shipmentNo, "GET_TRACKING", express.getExpressNo(), carrier, "SUCCESS", null);

        log.info(
                "获取快递单号: shipment={}, express={}, tracking={}",
                shipmentNo,
                express.getExpressNo(),
                trackingNo);
        return express;
    }

    /** 批量获取快递单号（高并发场景） 架构决策: 使用Kafka消息队列异步处理，避免阻塞 TODO: 发送Kafka消息，消费者批量调用承运商API */
    public List<String> batchGetTrackingNo(List<String> shipmentNos, String carrier) {
        log.info("批量获取快递单号: {}个, carrier={}", shipmentNos.size(), carrier);
        // TODO: 发送Kafka消息异步处理
        // 简化处理，返回处理中的单号列表
        return shipmentNos;
    }

    // ============================================================

    // 4. 快递单打印（高并发场景）
    // ============================================================

    /** 打印单个快递单 */
    @Transactional(rollbackFor = Exception.class)
    public ExpressOrder printExpress(String expressNo, String operator) {
        ExpressOrder express = expressOrderMapper.selectByExpressNo(expressNo);
        if (express == null) throw new RuntimeException("快递单不存在: " + expressNo);
        if (express.getTrackingNo() == null) {
            throw new RuntimeException("快递单未获取运单号: " + expressNo);
        }

        express.setStatus("PRINTED");
        express.setPrintTime(LocalDateTime.now());
        expressOrderMapper.updateById(express);

        // 创建作业记录
        createTask(
                express.getShipmentNo(),
                "PRINT",
                expressNo,
                express.getCarrier(),
                "SUCCESS",
                operator);

        // 更新发运单状态
        Shipment shipment = shipmentMapper.selectByShipmentNo(express.getShipmentNo());
        if (shipment != null && "CREATED".equals(shipment.getStatus())) {
            shipment.setStatus("PRINTED");
            shipmentMapper.updateById(shipment);
        }

        log.info("打印快递单: express={}, tracking={}", expressNo, express.getTrackingNo());
        return express;
    }

    /** 批量打印快递单（高并发场景） 架构决策: 使用Redis List异步处理，线程池消费 TODO: 将打印任务推入Redis List，消费者批量打印 */
    public List<String> batchPrintExpress(List<String> expressNos, String operator) {
        log.info("批量打印快递单: {}个", expressNos.size());
        // TODO: 推入Redis List异步处理
        // 简化处理，返回处理中的单号列表
        return expressNos;
    }

    // ============================================================

    // 5. 发运确认
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Shipment confirmShipment(String shipmentNo, String operator) {
        Shipment shipment = shipmentMapper.selectByShipmentNo(shipmentNo);
        if (shipment == null) throw new RuntimeException("发运单不存在: " + shipmentNo);

        shipment.setStatus("PICKED_UP");
        shipment.setShipTime(LocalDateTime.now());
        shipmentMapper.updateById(shipment);

        // 更新明细状态
        List<ShipmentDetail> details = detailMapper.selectByShipmentNo(shipmentNo);
        for (ShipmentDetail detail : details) {
            detail.setStatus("SHIPPED");
            detailMapper.updateById(detail);
        }

        // 更新快递单状态
        List<ExpressOrder> expresses = expressOrderMapper.selectByShipmentNo(shipmentNo);
        for (ExpressOrder express : expresses) {
            express.setStatus("PICKED_UP");
            express.setPickupTime(LocalDateTime.now());
            expressOrderMapper.updateById(express);
        }

        // 创建作业记录
        createTask(shipmentNo, "PICKUP", null, shipment.getCarrier(), "SUCCESS", operator);

        log.info("发运确认: shipment={}, carrier={}", shipmentNo, shipment.getCarrier());
        return shipment;
    }

    // ============================================================

    // 6. 快递状态回传
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ExpressOrder updateExpressStatus(String trackingNo, String status, String errorMsg) {
        ExpressOrder express = expressOrderMapper.selectByTrackingNo(trackingNo);
        if (express == null) throw new RuntimeException("快递单不存在: " + trackingNo);

        express.setStatus(status);
        if ("DELIVERED".equals(status)) {
            express.setDeliveryTime(LocalDateTime.now());
        }
        if ("FAILED".equals(status)) {
            express.setErrorMsg(errorMsg);
            express.setRetryCount(express.getRetryCount() + 1);
        }
        expressOrderMapper.updateById(express);

        // 更新发运单状态
        Shipment shipment = shipmentMapper.selectByShipmentNo(express.getShipmentNo());
        if (shipment != null) {
            if ("IN_TRANSIT".equals(status)) {
                shipment.setStatus("IN_TRANSIT");
            } else if ("DELIVERED".equals(status)) {
                shipment.setStatus("DELIVERED");
                shipment.setDeliveryTime(LocalDateTime.now());
            } else if ("FAILED".equals(status)) {
                shipment.setStatus("FAILED");
            }
            shipmentMapper.updateById(shipment);
        }

        log.info("快递状态回传: tracking={}, status={}", trackingNo, status);
        return express;
    }

    // ============================================================

    // 7. 重试机制
    // ============================================================

    /** 重试失败的快递单获取/打印 */
    @Transactional(rollbackFor = Exception.class)
    public int retryFailedExpress(String carrier) {
        List<ExpressOrder> failed = expressOrderMapper.selectByStatus("FAILED");
        int retryCount = 0;
        for (ExpressOrder express : failed) {
            if (express.getRetryCount() < MAX_RETRY) {
                // TODO: 重新调用承运商API
                express.setRetryCount(express.getRetryCount() + 1);
                express.setStatus("CREATED");
                express.setErrorMsg(null);
                expressOrderMapper.updateById(express);
                retryCount++;
            }
        }
        log.info("重试失败快递单: carrier={}, 重试{}个", carrier, retryCount);
        return retryCount;
    }

    // ============================================================

    // 8. 查询
    // ============================================================

    public Page<Shipment> pageShipments(
            Page<Shipment> page,
            String outboundNo,
            String carrier,
            String status,
            String warehouseCode) {
        LambdaQueryWrapper<Shipment> wrapper = new LambdaQueryWrapper<>();
        if (outboundNo != null) wrapper.eq(Shipment::getOutboundNo, outboundNo);
        if (carrier != null) wrapper.eq(Shipment::getCarrier, carrier);
        if (status != null) wrapper.eq(Shipment::getStatus, status);
        if (warehouseCode != null) wrapper.eq(Shipment::getWarehouseCode, warehouseCode);
        wrapper.orderByDesc(Shipment::getCreatedTime);
        return shipmentMapper.selectPage(page, wrapper);
    }

    public Shipment getShipmentByNo(String shipmentNo) {
        return shipmentMapper.selectByShipmentNo(shipmentNo);
    }

    public List<ShipmentDetail> getShipmentDetails(String shipmentNo) {
        return detailMapper.selectByShipmentNo(shipmentNo);
    }

    public List<ExpressOrder> getExpressOrders(String shipmentNo) {
        return expressOrderMapper.selectByShipmentNo(shipmentNo);
    }

    public ExpressOrder getExpressByTrackingNo(String trackingNo) {
        return expressOrderMapper.selectByTrackingNo(trackingNo);
    }

    public List<ShipmentTask> getShipmentTasks(String shipmentNo) {
        return taskMapper.selectByShipmentNo(shipmentNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private void createTask(
            String shipmentNo,
            String taskType,
            String expressNo,
            String carrier,
            String status,
            String operator) {
        ShipmentTask task = new ShipmentTask();
        task.setTaskNo(generateTaskNo());
        task.setShipmentNo(shipmentNo);
        task.setTaskType(taskType);
        task.setExpressNo(expressNo);
        task.setCarrier(carrier);
        task.setStatus(status);
        task.setRetryCount(0);
        task.setOperator(operator);
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now());
        taskMapper.insert(task);
    }

    private String generateShipmentNo() {
        return "SH"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateExpressNo() {
        return "EXP"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskNo() {
        return "SHT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
