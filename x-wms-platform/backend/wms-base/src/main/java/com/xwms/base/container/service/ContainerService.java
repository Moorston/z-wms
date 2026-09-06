package com.xwms.base.container.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.base.container.entity.*;
import com.xwms.base.container.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 容器管理核心服务 核心能力: 容器档案/容器绑定/容器释放/容器移动/容器追踪 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerService {

    private final ContainerTypeMapper typeMapper;
    private final ContainerMapper containerMapper;
    private final ContainerBindMapper bindMapper;
    private final ContainerTraceMapper traceMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================
    // 1. 容器类型管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ContainerType createType(ContainerType type) {
        type.setStatus("ACTIVE");
        typeMapper.insert(type);
        log.info("创建容器类型: {}={}", type.getTypeCode(), type.getTypeName());
        return type;
    }

    public Page<ContainerType> pageTypes(Page<ContainerType> page, String category) {
        LambdaQueryWrapper<ContainerType> wrapper = new LambdaQueryWrapper<>();
        if (category != null) wrapper.eq(ContainerType::getCategory, category);
        wrapper.eq(ContainerType::getStatus, "ACTIVE");
        return typeMapper.selectPage(page, wrapper);
    }

    public ContainerType getTypeByCode(String typeCode) {
        return typeMapper.selectByTypeCode(typeCode);
    }

    // ============================================================
    // 2. 容器档案管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Container createContainer(Container container) {
        if (container.getStatus() == null) container.setStatus("EMPTY");
        if (container.getCurrentLoad() == null) container.setCurrentLoad(BigDecimal.ZERO);
        if (container.getCurrentVolume() == null) container.setCurrentVolume(BigDecimal.ZERO);
        if (container.getSkuCount() == null) container.setSkuCount(0);
        if (container.getItemCount() == null) container.setItemCount(0);
        if (container.getBatchCount() == null) container.setBatchCount(0);
        if (container.getUseCount() == null) container.setUseCount(0);
        containerMapper.insert(container);
        log.info("创建容器: {}={}", container.getContainerNo(), container.getTypeCode());
        return container;
    }

    public Page<Container> pageContainers(
            Page<Container> page,
            String typeCode,
            String status,
            String location,
            String warehouseCode) {
        LambdaQueryWrapper<Container> wrapper = new LambdaQueryWrapper<>();
        if (typeCode != null) wrapper.eq(Container::getTypeCode, typeCode);
        if (status != null) wrapper.eq(Container::getStatus, status);
        if (location != null) wrapper.eq(Container::getCurrentLocation, location);
        if (warehouseCode != null) wrapper.eq(Container::getWarehouseCode, warehouseCode);
        wrapper.orderByAsc(Container::getContainerNo);
        return containerMapper.selectPage(page, wrapper);
    }

    public Container getContainerByNo(String containerNo) {
        return containerMapper.selectByContainerNo(containerNo);
    }

    /** 获取空容器 */
    public Container getEmptyContainer(String typeCode, String warehouseCode) {
        return containerMapper.selectEmptyContainer(typeCode, warehouseCode);
    }

    // ============================================================
    // 3. 容器绑定（商品装入容器）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ContainerBind bindContainer(
            String containerNo,
            String refType,
            String refNo,
            String skuCode,
            String batchNo,
            BigDecimal quantity,
            String operator) {
        Container container = containerMapper.selectByContainerNo(containerNo);
        if (container == null) throw new RuntimeException("容器不存在: " + containerNo);
        if (!"EMPTY".equals(container.getStatus()) && !"OCCUPIED".equals(container.getStatus())) {
            throw new RuntimeException("容器状态不正确: " + container.getStatus());
        }

        // 创建绑定记录
        ContainerBind bind = new ContainerBind();
        bind.setBindNo(generateBindNo());
        bind.setContainerNo(containerNo);
        bind.setRefType(refType);
        bind.setRefNo(refNo);
        bind.setSkuCode(skuCode);
        bind.setBatchNo(batchNo);
        bind.setQuantity(quantity);
        bind.setStatus("BOUND");
        bind.setBindTime(LocalDateTime.now());
        bind.setOperator(operator);
        bindMapper.insert(bind);

        // 更新容器状态
        container.setStatus("OCCUPIED");
        container.setCurrentLoad(container.getCurrentLoad().add(quantity));
        container.setSkuCount(container.getSkuCount() + 1);
        container.setItemCount(container.getItemCount() + quantity.intValue());
        container.setBatchCount(container.getBatchCount() + 1);
        container.setUseCount(container.getUseCount() + 1);
        containerMapper.updateById(container);

        // 记录流转
        recordTrace(
                containerNo,
                "BIND",
                container.getCurrentLocation(),
                container.getCurrentLocation(),
                refType,
                refNo,
                quantity,
                operator,
                "绑定商品");

        log.info(
                "容器绑定: container={}, ref={}/{}, sku={}, qty={}",
                containerNo,
                refType,
                refNo,
                skuCode,
                quantity);
        return bind;
    }

    // ============================================================
    // 4. 容器释放（商品从容器取出）
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public ContainerBind releaseContainer(
            String containerNo,
            String refType,
            String refNo,
            BigDecimal quantity,
            String operator) {
        List<ContainerBind> binds = bindMapper.selectActiveByContainer(containerNo);
        ContainerBind targetBind = null;
        for (ContainerBind bind : binds) {
            if (refType.equals(bind.getRefType()) && refNo.equals(bind.getRefNo())) {
                targetBind = bind;
                break;
            }
        }
        if (targetBind == null) {
            throw new RuntimeException("未找到绑定记录: " + containerNo + "/" + refType + "/" + refNo);
        }

        // 释放绑定
        targetBind.setStatus("RELEASED");
        targetBind.setReleaseTime(LocalDateTime.now());
        bindMapper.updateById(targetBind);

        // 更新容器状态
        Container container = containerMapper.selectByContainerNo(containerNo);
        container.setCurrentLoad(container.getCurrentLoad().subtract(quantity));
        container.setItemCount(container.getItemCount() - quantity.intValue());

        // 检查是否还有其他绑定
        List<ContainerBind> remainingBinds = bindMapper.selectActiveByContainer(containerNo);
        if (remainingBinds.isEmpty()) {
            container.setStatus("EMPTY");
            container.setSkuCount(0);
            container.setBatchCount(0);
            container.setCurrentLoad(BigDecimal.ZERO);
            container.setCurrentVolume(BigDecimal.ZERO);
        } else {
            container.setSkuCount(remainingBinds.size());
        }
        containerMapper.updateById(container);

        // 记录流转
        recordTrace(
                containerNo,
                "RELEASE",
                container.getCurrentLocation(),
                container.getCurrentLocation(),
                refType,
                refNo,
                quantity,
                operator,
                "释放商品");

        log.info("容器释放: container={}, ref={}/{}, qty={}", containerNo, refType, refNo, quantity);
        return targetBind;
    }

    // ============================================================
    // 5. 容器移动
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Container moveContainer(
            String containerNo, String fromLocation, String toLocation, String operator) {
        Container container = containerMapper.selectByContainerNo(containerNo);
        if (container == null) throw new RuntimeException("容器不存在: " + containerNo);

        container.setCurrentLocation(toLocation);
        containerMapper.updateById(container);

        // 记录流转
        recordTrace(
                containerNo, "MOVE", fromLocation, toLocation, null, null, null, operator, "容器移动");

        log.info("容器移动: container={}, {}->{}", containerNo, fromLocation, toLocation);
        return container;
    }

    // ============================================================
    // 6. 容器清洁/维修/报废
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public Container cleanContainer(String containerNo, String operator) {
        Container container = containerMapper.selectByContainerNo(containerNo);
        if (container == null) throw new RuntimeException("容器不存在: " + containerNo);

        container.setLastCleanTime(LocalDateTime.now());
        containerMapper.updateById(container);

        recordTrace(
                containerNo,
                "CLEAN",
                container.getCurrentLocation(),
                container.getCurrentLocation(),
                null,
                null,
                null,
                operator,
                "容器清洁");

        log.info("容器清洁: {}", containerNo);
        return container;
    }

    @Transactional(rollbackFor = Exception.class)
    public Container repairContainer(String containerNo, String operator) {
        Container container = containerMapper.selectByContainerNo(containerNo);
        if (container == null) throw new RuntimeException("容器不存在: " + containerNo);

        container.setStatus("REPAIR");
        containerMapper.updateById(container);

        recordTrace(
                containerNo,
                "REPAIR",
                container.getCurrentLocation(),
                container.getCurrentLocation(),
                null,
                null,
                null,
                operator,
                "容器维修");

        log.info("容器维修: {}", containerNo);
        return container;
    }

    @Transactional(rollbackFor = Exception.class)
    public Container discardContainer(String containerNo, String operator) {
        Container container = containerMapper.selectByContainerNo(containerNo);
        if (container == null) throw new RuntimeException("容器不存在: " + containerNo);

        container.setStatus("DISCARD");
        containerMapper.updateById(container);

        recordTrace(
                containerNo,
                "DISCARD",
                container.getCurrentLocation(),
                container.getCurrentLocation(),
                null,
                null,
                null,
                operator,
                "容器报废");

        log.info("容器报废: {}", containerNo);
        return container;
    }

    // ============================================================
    // 7. 容器追踪
    // ============================================================

    public List<ContainerTrace> getContainerTrace(String containerNo) {
        return traceMapper.selectByContainerNo(containerNo);
    }

    public List<ContainerBind> getContainerBinds(String containerNo) {
        return bindMapper.selectActiveByContainer(containerNo);
    }

    public List<ContainerBind> getBindsByRef(String refType, String refNo) {
        return bindMapper.selectByRef(refType, refNo);
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private void recordTrace(
            String containerNo,
            String actionType,
            String fromLocation,
            String toLocation,
            String refType,
            String refNo,
            BigDecimal quantity,
            String operator,
            String remark) {
        ContainerTrace trace = new ContainerTrace();
        trace.setTraceNo(generateTraceNo());
        trace.setContainerNo(containerNo);
        trace.setActionType(actionType);
        trace.setFromLocation(fromLocation);
        trace.setToLocation(toLocation);
        trace.setRefType(refType);
        trace.setRefNo(refNo);
        trace.setQuantity(quantity);
        trace.setOperator(operator);
        trace.setActionTime(LocalDateTime.now());
        trace.setRemark(remark);
        traceMapper.insert(trace);
    }

    private String generateBindNo() {
        return "CB"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTraceNo() {
        return "CT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
