package com.xwms.core.vas.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.vas.dto.VasOrderCreateRequest;
import com.xwms.core.vas.entity.*;
import com.xwms.core.vas.enums.VasOrderStatus;
import com.xwms.core.vas.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** VAS增值服务核心服务 包含: 服务定义管理/工单创建/工单分配/工单执行/物料领用/工单完成 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VasOrderService {

    private final VasServiceMapper serviceMapper;
    private final VasOrderMapper orderMapper;
    private final VasOrderItemMapper itemMapper;
    private final VasMaterialMapper materialMapper;

    private static final AtomicInteger ORDER_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. VAS服务定义管理
    // ============================================================

    public VasService createService(VasService service) {
        service.setStatus("ENABLED");
        serviceMapper.insert(service);
        log.info("创建VAS服务: {}", service.getServiceCode());
        return service;
    }

    public VasService getService(Long id) {
        VasService service = serviceMapper.selectById(id);
        if (service == null) throw new BizException("VAS服务不存在: " + id);
        return service;
    }

    public Page<VasService> pageServices(Page<VasService> page, String type, String status) {
        LambdaQueryWrapper<VasService> wrapper = new LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(VasService::getServiceType, type);
        if (status != null) wrapper.eq(VasService::getStatus, status);
        wrapper.orderByDesc(VasService::getCreatedAt);
        return serviceMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 创建VAS工单
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasOrder createOrder(VasOrderCreateRequest request) {
        // 查找服务定义
        VasService service =
                serviceMapper.selectOne(
                        new LambdaQueryWrapper<VasService>()
                                .eq(VasService::getServiceCode, request.getServiceCode())
                                .eq(VasService::getStatus, "ENABLED"));
        if (service == null) throw new BizException("VAS服务不存在或已禁用: " + request.getServiceCode());

        String orderNo = generateOrderNo();

        VasOrder order = new VasOrder();
        order.setOrderNo(orderNo);
        order.setOrderType(request.getOrderType());
        order.setSourceOrderNo(request.getSourceOrderNo());
        order.setSourceOrderType(request.getSourceOrderType());
        order.setCustomerCode(request.getCustomerCode());
        order.setOwnerCode(request.getOwnerCode());
        order.setWarehouseCode(request.getWarehouseCode());
        order.setServiceCode(service.getServiceCode());
        order.setServiceName(service.getServiceName());
        order.setServiceType(service.getServiceType());
        order.setPlanQty(request.getPlanQty());
        order.setActualQty(BigDecimal.ZERO);
        order.setUnit(service.getUnit());
        order.setUnitPrice(service.getUnitPrice());
        order.setTotalAmount(
                service.getUnitPrice() != null && request.getPlanQty() != null
                        ? service.getUnitPrice().multiply(request.getPlanQty())
                        : BigDecimal.ZERO);
        order.setStatus(VasOrderStatus.PENDING.getCode());
        order.setPriority(request.getPriority() != null ? request.getPriority() : 5);
        order.setLocationCode(request.getLocationCode());
        order.setBatchNo(request.getBatchNo());
        order.setPlanStartTime(request.getPlanStartTime());
        order.setPlanFinishTime(request.getPlanFinishTime());
        order.setOwnerCodeCol(request.getOwnerCode());
        order.setWarehouseCodeCol(request.getWarehouseCode());

        orderMapper.insert(order);

        // 创建工单明细
        if (request.getItems() != null) {
            for (var itemReq : request.getItems()) {
                VasOrderItem item = new VasOrderItem();
                item.setOrderId(order.getId());
                item.setOrderNo(orderNo);
                item.setSku(itemReq.getSku());
                item.setBarcode(itemReq.getBarcode());
                item.setProductName(itemReq.getProductName());
                item.setBatchNo(itemReq.getBatchNo());
                item.setOwnerCode(request.getOwnerCode());
                item.setFromLocation(itemReq.getFromLocation());
                item.setToLocation(itemReq.getToLocation());
                item.setPlanQty(itemReq.getPlanQty());
                item.setActualQty(BigDecimal.ZERO);
                item.setUnitPrice(service.getUnitPrice());
                item.setItemAmount(
                        service.getUnitPrice() != null && itemReq.getPlanQty() != null
                                ? service.getUnitPrice().multiply(itemReq.getPlanQty())
                                : BigDecimal.ZERO);
                item.setItemStatus("PENDING");
                item.setBeforeSpec(itemReq.getBeforeSpec());
                item.setOwnerCodeCol(request.getOwnerCode());
                item.setWarehouseCodeCol(request.getWarehouseCode());
                itemMapper.insert(item);
            }
        }

        // 创建物料需求(如果服务需要物料)
        if (service.getNeedMaterial() != null
                && service.getNeedMaterial() == 1
                && service.getMaterialList() != null) {
            // TODO: 解析materialList JSON, 创建物料需求记录
            log.info("VAS服务{}需要物料, 待创建物料需求", service.getServiceCode());
        }

        log.info(
                "创建VAS工单: {}, 服务={}, 数量={}",
                orderNo,
                service.getServiceCode(),
                request.getPlanQty());
        return order;
    }

    // ============================================================

    // 3. 工单分配
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasOrder assignOrder(Long orderId, String assignee) {
        VasOrder order = getOrder(orderId);
        if (!VasOrderStatus.PENDING.getCode().equals(order.getStatus())) {
            throw new BizException("工单状态不允许分配: " + order.getStatus());
        }
        order.setStatus(VasOrderStatus.ASSIGNED.getCode());
        order.setAssignee(assignee);
        order.setAssignTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("VAS工单{}分配给: {}", order.getOrderNo(), assignee);
        return order;
    }

    // ============================================================

    // 4. 工单开始执行
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasOrder startOrder(Long orderId) {
        VasOrder order = getOrder(orderId);
        if (!VasOrderStatus.ASSIGNED.getCode().equals(order.getStatus())
                && !VasOrderStatus.PENDING.getCode().equals(order.getStatus())) {
            throw new BizException("工单状态不允许开始: " + order.getStatus());
        }
        order.setStatus(VasOrderStatus.PROCESSING.getCode());
        order.setStartTime(LocalDateTime.now());
        orderMapper.updateById(order);

        // 更新明细状态
        List<VasOrderItem> items = itemMapper.selectByOrderId(orderId);
        for (VasOrderItem item : items) {
            if ("PENDING".equals(item.getItemStatus())) {
                item.setItemStatus("PROCESSING");
                itemMapper.updateById(item);
            }
        }

        log.info("VAS工单{}开始执行", order.getOrderNo());
        return order;
    }

    // ============================================================

    // 5. 工单暂停/恢复
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasOrder pauseOrder(Long orderId, String reason) {
        VasOrder order = getOrder(orderId);
        if (!VasOrderStatus.PROCESSING.getCode().equals(order.getStatus())) {
            throw new BizException("只有处理中状态才能暂停: " + order.getStatus());
        }
        order.setStatus(VasOrderStatus.PAUSED.getCode());
        order.setRemark(reason);
        orderMapper.updateById(order);
        log.info("VAS工单{}暂停: {}", order.getOrderNo(), reason);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public VasOrder resumeOrder(Long orderId) {
        VasOrder order = getOrder(orderId);
        if (!VasOrderStatus.PAUSED.getCode().equals(order.getStatus())) {
            throw new BizException("只有暂停状态才能恢复: " + order.getStatus());
        }
        order.setStatus(VasOrderStatus.PROCESSING.getCode());
        orderMapper.updateById(order);
        log.info("VAS工单{}恢复执行", order.getOrderNo());
        return order;
    }

    // ============================================================

    // 6. 工单明细完成
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasOrderItem completeItem(Long itemId, BigDecimal actualQty, String afterSpec) {
        VasOrderItem item = itemMapper.selectById(itemId);
        if (item == null) throw new BizException("工单明细不存在");
        if (!"PROCESSING".equals(item.getItemStatus())) {
            throw new BizException("明细状态不允许完成: " + item.getItemStatus());
        }

        item.setActualQty(actualQty);
        item.setAfterSpec(afterSpec);
        item.setItemStatus("COMPLETED");
        itemMapper.updateById(item);

        // 检查是否所有明细都完成
        checkOrderComplete(item.getOrderId());

        log.info("VAS工单明细{}完成: 实际数量={}", itemId, actualQty);
        return item;
    }

    /** 检查工单是否所有明细都完成, 如果是则自动完成工单 */
    private void checkOrderComplete(Long orderId) {
        List<VasOrderItem> items = itemMapper.selectByOrderId(orderId);
        boolean allComplete = items.stream().allMatch(i -> "COMPLETED".equals(i.getItemStatus()));
        if (allComplete && !items.isEmpty()) {
            VasOrder order = getOrder(orderId);
            BigDecimal totalActual =
                    items.stream()
                            .map(VasOrderItem::getActualQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setActualQty(totalActual);
            order.setStatus(VasOrderStatus.COMPLETED.getCode());
            order.setFinishTime(LocalDateTime.now());
            // 计算实际耗时
            if (order.getStartTime() != null) {
                long minutes =
                        java.time.Duration.between(order.getStartTime(), LocalDateTime.now())
                                .toMinutes();
                order.setActualDuration(new BigDecimal(minutes));
            }
            orderMapper.updateById(order);
            log.info("VAS工单{}自动完成", order.getOrderNo());
        }
    }

    // ============================================================

    // 7. 工单完成
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasOrder completeOrder(Long orderId, BigDecimal actualQty) {
        VasOrder order = getOrder(orderId);
        if (!VasOrderStatus.PROCESSING.getCode().equals(order.getStatus())
                && !VasOrderStatus.PAUSED.getCode().equals(order.getStatus())) {
            throw new BizException("工单状态不允许完成: " + order.getStatus());
        }

        order.setActualQty(actualQty != null ? actualQty : order.getPlanQty());
        order.setStatus(VasOrderStatus.COMPLETED.getCode());
        order.setFinishTime(LocalDateTime.now());
        if (order.getStartTime() != null) {
            long minutes =
                    java.time.Duration.between(order.getStartTime(), LocalDateTime.now())
                            .toMinutes();
            order.setActualDuration(new BigDecimal(minutes));
        }
        // 更新总费用
        if (order.getUnitPrice() != null && order.getActualQty() != null) {
            order.setTotalAmount(order.getUnitPrice().multiply(order.getActualQty()));
        }
        orderMapper.updateById(order);

        // 更新所有明细为完成
        List<VasOrderItem> items = itemMapper.selectByOrderId(orderId);
        for (VasOrderItem item : items) {
            if (!"COMPLETED".equals(item.getItemStatus())) {
                item.setItemStatus("COMPLETED");
                item.setActualQty(item.getPlanQty());
                itemMapper.updateById(item);
            }
        }

        // 物料消耗确认
        confirmMaterials(orderId);

        log.info(
                "VAS工单{}完成: 实际数量={}, 耗时={}分钟",
                order.getOrderNo(),
                order.getActualQty(),
                order.getActualDuration());
        return order;
    }

    // ============================================================

    // 8. 物料领用
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasMaterial pickMaterial(Long materialId, String pickBy) {
        VasMaterial material = materialMapper.selectById(materialId);
        if (material == null) throw new BizException("物料记录不存在");
        if (!"PENDING".equals(material.getStatus())) {
            throw new BizException("物料状态不允许领用: " + material.getStatus());
        }
        material.setStatus("PICKED");
        material.setPickBy(pickBy);
        material.setPickTime(LocalDateTime.now());
        materialMapper.updateById(material);
        log.info("VAS物料领用: {}, 领用人={}", material.getMaterialSku(), pickBy);
        return material;
    }

    @Transactional(rollbackFor = Exception.class)
    public VasMaterial consumeMaterial(Long materialId, BigDecimal actualQty) {
        VasMaterial material = materialMapper.selectById(materialId);
        if (material == null) throw new BizException("物料记录不存在");
        material.setActualQty(actualQty);
        material.setStatus("CONSUMED");
        material.setTotalCost(
                material.getUnitCost() != null
                        ? material.getUnitCost().multiply(actualQty)
                        : BigDecimal.ZERO);
        materialMapper.updateById(material);
        log.info("VAS物料消耗: {}, 实际用量={}", material.getMaterialSku(), actualQty);
        return material;
    }

    private void confirmMaterials(Long orderId) {
        List<VasMaterial> materials = materialMapper.selectByOrderId(orderId);
        for (VasMaterial material : materials) {
            if ("PICKED".equals(material.getStatus())) {
                material.setStatus("CONSUMED");
                material.setActualQty(material.getPlanQty());
                materialMapper.updateById(material);
            }
        }
    }

    // ============================================================

    // 9. 工单取消/异常
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public VasOrder cancelOrder(Long orderId, String reason) {
        VasOrder order = getOrder(orderId);
        if (VasOrderStatus.COMPLETED.getCode().equals(order.getStatus())) {
            throw new BizException("已完成的工单不能取消");
        }
        order.setStatus(VasOrderStatus.CANCELLED.getCode());
        order.setRemark(reason);
        orderMapper.updateById(order);
        log.info("VAS工单{}取消: {}", order.getOrderNo(), reason);
        return order;
    }

    @Transactional(rollbackFor = Exception.class)
    public VasOrder exceptionOrder(Long orderId, String reason) {
        VasOrder order = getOrder(orderId);
        order.setStatus(VasOrderStatus.EXCEPTION.getCode());
        order.setExceptionReason(reason);
        orderMapper.updateById(order);
        log.warn("VAS工单{}异常: {}", order.getOrderNo(), reason);
        return order;
    }

    // ============================================================

    // 10. 查询
    // ============================================================

    public VasOrder getOrder(Long id) {
        VasOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException("VAS工单不存在: " + id);
        return order;
    }

    public Page<VasOrder> pageOrders(
            Page<VasOrder> page, String status, String type, String serviceCode) {
        LambdaQueryWrapper<VasOrder> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(VasOrder::getStatus, status);
        if (type != null) wrapper.eq(VasOrder::getOrderType, type);
        if (serviceCode != null) wrapper.eq(VasOrder::getServiceCode, serviceCode);
        wrapper.orderByAsc(VasOrder::getPriority).orderByDesc(VasOrder::getCreatedAt);
        return orderMapper.selectPage(page, wrapper);
    }

    public List<VasOrderItem> getOrderItems(Long orderId) {
        return itemMapper.selectByOrderId(orderId);
    }

    public List<VasMaterial> getOrderMaterials(Long orderId) {
        return materialMapper.selectByOrderId(orderId);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateOrderNo() {
        return "VAS"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", ORDER_SEQ.incrementAndGet() % 1000);
    }
}
