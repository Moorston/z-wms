package com.xwms.core.putaway.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.inbound.entity.InboundDetail;
import com.xwms.core.inbound.entity.InboundOrder;
import com.xwms.core.inbound.mapper.InboundDetailMapper;
import com.xwms.core.inbound.mapper.InboundOrderMapper;
import com.xwms.core.putaway.entity.*;
import com.xwms.core.putaway.enums.PutawayStatus;
import com.xwms.core.putaway.enums.PutawayType;
import com.xwms.core.putaway.mapper.*;
import com.xwms.core.putawayrule.entity.PutawayRule;
import com.xwms.core.putawayrule.service.LocationQueryService;
import com.xwms.core.putawayrule.service.PutawayRuleService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 上架管理核心服务 支持7种上架方式： C1 标准上架 / C2 快捷上架 / C3 合并上架 / C4 批量上架 C5 按箱码/LPN上架 / C6 直接收货到存储库位 / C7 码盘预约库位
 * 集成6种上架策略算法：NEAREST/FIFO/FEFO/ZONE/HEIGHT/WEIGHT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PutawayService {

    private final PutawayTaskMapper taskMapper;
    private final PutawayTaskDetailMapper taskDetailMapper;
    private final PutawayRecordMapper recordMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final InboundDetailMapper inboundDetailMapper;
    private final PutawayRuleService putawayRuleService;
    private final LocationQueryService locationQueryService;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 上架任务管理
    // ============================================================

    /** 从收货完成的入库单生成上架任务 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTask createTaskFromInbound(
            String inboundNo, String putawayType, String putawayStrategy, String operator) {
        log.info(
                "从入库单生成上架任务: inboundNo={}, type={}, strategy={}",
                inboundNo,
                putawayType,
                putawayStrategy);

        InboundOrder inboundOrder = inboundOrderMapper.selectByInboundNo(inboundNo);
        if (inboundOrder == null) throw new RuntimeException("入库单不存在: " + inboundNo);

        // 检查是否已有上架任务
        PutawayTask existing = taskMapper.selectByInboundNo(inboundNo);
        if (existing != null && !PutawayStatus.CANCELLED.getCode().equals(existing.getStatus())) {
            log.warn("入库单已有上架任务: inboundNo={}, taskNo={}", inboundNo, existing.getTaskNo());
            return existing;
        }

        // 创建上架任务
        PutawayTask task = new PutawayTask();
        task.setTaskNo(generateTaskNo());
        task.setInboundNo(inboundNo);
        task.setAsnNo(inboundOrder.getAsnNo());
        task.setPutawayType(putawayType != null ? putawayType : PutawayType.STANDARD.getCode());
        task.setPutawayStrategy(putawayStrategy != null ? putawayStrategy : "NEAREST");
        task.setSupplierCode(inboundOrder.getSupplierCode());
        task.setOwnerCode(inboundOrder.getOwnerCodeCol());
        task.setWarehouseCode(inboundOrder.getWarehouseCode());
        task.setExpectedQty(
                inboundOrder.getReceivedQty() != null
                        ? inboundOrder.getReceivedQty()
                        : BigDecimal.ZERO);
        task.setPutawayQty(BigDecimal.ZERO);
        task.setDifferenceQty(BigDecimal.ZERO);
        task.setStatus(PutawayStatus.PENDING.getCode());
        task.setSystemRecommend("Y");
        task.setAllowLocationChange("Y");
        task.setAllowQtyChange("Y");
        task.setSource("RECEIPT");
        task.setCreatedBy(operator);
        taskMapper.insert(task);

        // 从入库单明细生成上架任务明细，并推荐库位
        List<InboundDetail> inboundDetails = inboundDetailMapper.selectByInboundNo(inboundNo);
        int lineNo = 1;
        for (InboundDetail inboundDetail : inboundDetails) {
            if (inboundDetail.getReceivedQty() == null
                    || inboundDetail.getReceivedQty().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            PutawayTaskDetail detail = new PutawayTaskDetail();
            detail.setDetailNo(generateTaskDetailNo());
            detail.setTaskNo(task.getTaskNo());
            detail.setLineNo(lineNo++);
            detail.setInboundDetailNo(inboundDetail.getDetailNo());
            detail.setSkuCode(inboundDetail.getSkuCode());
            detail.setSkuName(inboundDetail.getSkuName());
            detail.setBarcode(inboundDetail.getBarcode());
            detail.setUnit(inboundDetail.getUnit());
            detail.setExpectedQty(inboundDetail.getReceivedQty());
            detail.setPutawayQty(BigDecimal.ZERO);
            detail.setDifferenceQty(BigDecimal.ZERO);
            detail.setSourceLocation(inboundDetail.getReceiveLocation());
            detail.setStatus(PutawayStatus.PENDING.getCode());

            // 推荐库位（集成上架规则）
            try {
                String recommendLocation = recommendLocation(detail, task.getPutawayStrategy());
                detail.setRecommendLocation(recommendLocation);
                log.info("推荐库位: sku={}, location={}", detail.getSkuCode(), recommendLocation);
            } catch (Exception e) {
                log.warn("推荐库位失败: sku={}, error={}", detail.getSkuCode(), e.getMessage());
            }

            taskDetailMapper.insert(detail);
        }

        // 更新入库单状态
        inboundOrder.setStatus("PUTAWAYING");
        inboundOrderMapper.updateById(inboundOrder);

        log.info("从入库单生成上架任务完成: taskNo={}, 明细{}行", task.getTaskNo(), lineNo - 1);
        return task;
    }

    /** 取消上架任务 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTask cancelTask(String taskNo, String cancelReason, String operator) {
        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("上架任务不存在: " + taskNo);

        if (task.getPutawayQty() != null && task.getPutawayQty().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("已上架的任务不允许取消: " + taskNo);
        }

        task.setStatus(PutawayStatus.CANCELLED.getCode());
        task.setRemark(cancelReason);
        task.setUpdatedBy(operator);
        task.setUpdatedTime(LocalDateTime.now());
        taskMapper.updateById(task);

        List<PutawayTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        for (PutawayTaskDetail detail : details) {
            detail.setStatus(PutawayStatus.CANCELLED.getCode());
            taskDetailMapper.updateById(detail);
        }

        log.info("取消上架任务: taskNo={}, 原因={}", taskNo, cancelReason);
        return task;
    }

    public PutawayTask getTaskByNo(String taskNo) {
        return taskMapper.selectByTaskNo(taskNo);
    }

    public List<PutawayTaskDetail> getTaskDetails(String taskNo) {
        return taskDetailMapper.selectByTaskNo(taskNo);
    }

    public Page<PutawayTask> pageTasks(
            Page<PutawayTask> page,
            String putawayType,
            String status,
            String warehouseCode,
            String inboundNo) {
        LambdaQueryWrapper<PutawayTask> wrapper = new LambdaQueryWrapper<>();
        if (putawayType != null) wrapper.eq(PutawayTask::getPutawayType, putawayType);
        if (status != null) wrapper.eq(PutawayTask::getStatus, status);
        if (warehouseCode != null) wrapper.eq(PutawayTask::getWarehouseCode, warehouseCode);
        if (inboundNo != null) wrapper.eq(PutawayTask::getInboundNo, inboundNo);
        wrapper.orderByDesc(PutawayTask::getCreatedTime);
        return taskMapper.selectPage(page, wrapper);
    }

    // ============================================================

    // 2. 库位推荐（核心功能，集成上架规则）
    // ============================================================

    /** 推荐库位 集成putawayrule模块的6种上架策略 */
    public String recommendLocation(PutawayTaskDetail detail, String strategy) {
        log.info("推荐库位: sku={}, strategy={}", detail.getSkuCode(), strategy);

        // 构建上架规则查询参数
        PutawayRule rule = new PutawayRule();
        rule.setSkuCode(detail.getSkuCode());
        rule.setWarehouseCode(detail.getTaskNo() != null ? "WH001" : null); // TODO: 从任务获取仓库
        rule.setStrategy(strategy);
        rule.setProductWeight(detail.getProductWeight());
        rule.setPreferredArea(detail.getRecommendArea());

        // 调用上架规则服务推荐库位
        List<com.xwms.core.putawayrule.entity.PutawayDetail> recommendations =
                putawayRuleService.recommendLocations(
                        detail.getSkuCode(),
                        null,
                        rule.getOwnerCode(),
                        rule.getWarehouseCode(),
                        detail.getBatchNo(),
                        detail.getExpectedQty());

        if (recommendations != null && !recommendations.isEmpty()) {
            return recommendations.get(0).getTargetLocation();
        }

        // 如果规则服务没有返回，使用LocationQueryService查询最近库位
        List<LocationQueryService.PutawayLocation> locations =
                locationQueryService.queryAvailableLocations(
                        rule.getWarehouseCode() != null ? rule.getWarehouseCode() : "WH001",
                        null,
                        "STORAGE",
                        null,
                        null,
                        detail.getProductWeight(),
                        true,
                        detail.getSkuCode(),
                        detail.getBatchNo());

        if (locations != null && !locations.isEmpty()) {
            return locations.get(0).getLocationCode();
        }

        log.warn("未找到推荐库位: sku={}", detail.getSkuCode());
        return null;
    }

    /** 重新推荐库位 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTaskDetail reRecommendLocation(
            String detailNo, String strategy, String operator) {
        PutawayTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new RuntimeException("上架任务明细不存在: " + detailNo);

        String newLocation = recommendLocation(detail, strategy);
        detail.setRecommendLocation(newLocation);
        detail.setUpdatedBy(operator);
        detail.setUpdatedTime(LocalDateTime.now());
        taskDetailMapper.updateById(detail);

        log.info("重新推荐库位: detailNo={}, newLocation={}", detailNo, newLocation);
        return detail;
    }

    // ============================================================

    // 3. C1 标准上架（系统推荐库位+扫描确认）
    // ============================================================

    /** 标准上架 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRecord standardPutaway(
            String taskNo,
            String detailNo,
            String targetLocation,
            BigDecimal putawayQty,
            String operator) {
        log.info(
                "标准上架: taskNo={}, detailNo={}, location={}, qty={}",
                taskNo,
                detailNo,
                targetLocation,
                putawayQty);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("上架任务不存在: " + taskNo);

        PutawayTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new RuntimeException("上架任务明细不存在: " + detailNo);

        // 校验数量
        BigDecimal remainingQty = detail.getExpectedQty().subtract(detail.getPutawayQty());
        if (putawayQty.compareTo(remainingQty) > 0) {
            throw new RuntimeException(
                    String.format(
                            "上架数量超过剩余数量: 明细=%s, 剩余=%s, 请求=%s", detailNo, remainingQty, putawayQty));
        }

        // 校验库位（如果不允许修改库位）
        if ("N".equals(task.getAllowLocationChange())
                && targetLocation != null
                && !targetLocation.equals(detail.getRecommendLocation())) {
            throw new RuntimeException("不允许修改推荐库位: 推荐=" + detail.getRecommendLocation());
        }

        // 创建上架记录
        PutawayRecord record =
                createPutawayRecord(
                        task,
                        detail,
                        putawayQty,
                        targetLocation,
                        PutawayType.STANDARD.getCode(),
                        operator);

        // 更新任务明细
        detail.setPutawayQty(detail.getPutawayQty().add(putawayQty));
        detail.setActualLocation(targetLocation);
        if (detail.getPutawayQty().compareTo(detail.getExpectedQty()) >= 0) {
            detail.setStatus(PutawayStatus.COMPLETED.getCode());
        } else {
            detail.setStatus(PutawayStatus.PUTAWAYING.getCode());
        }
        taskDetailMapper.updateById(detail);

        // 更新任务
        updateTaskAfterPutaway(task, putawayQty);

        // 更新入库单
        updateInboundAfterPutaway(task, putawayQty);

        log.info("标准上架完成: taskNo={}, detailNo={}, qty={}", taskNo, detailNo, putawayQty);
        return record;
    }

    // ============================================================

    // 4. C2 快捷上架（简化版）
    // ============================================================

    /** 快捷上架（使用推荐库位，默认全部上架） */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTask quickPutaway(String taskNo, String operator) {
        log.info("快捷上架: taskNo={}", taskNo);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("上架任务不存在: " + taskNo);

        List<PutawayTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        BigDecimal totalPutawayQty = BigDecimal.ZERO;

        for (PutawayTaskDetail detail : details) {
            if (PutawayStatus.COMPLETED.getCode().equals(detail.getStatus())) continue;

            BigDecimal putawayQty = detail.getExpectedQty().subtract(detail.getPutawayQty());
            if (putawayQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            String targetLocation =
                    detail.getRecommendLocation() != null
                            ? detail.getRecommendLocation()
                            : detail.getSourceLocation();

            // 创建上架记录
            createPutawayRecord(
                    task,
                    detail,
                    putawayQty,
                    targetLocation,
                    PutawayType.QUICK.getCode(),
                    operator);

            // 更新明细
            detail.setPutawayQty(detail.getExpectedQty());
            detail.setActualLocation(targetLocation);
            detail.setStatus(PutawayStatus.COMPLETED.getCode());
            taskDetailMapper.updateById(detail);

            totalPutawayQty = totalPutawayQty.add(putawayQty);
        }

        // 更新任务
        updateTaskAfterPutaway(task, totalPutawayQty);

        // 更新入库单
        updateInboundAfterPutaway(task, totalPutawayQty);

        log.info("快捷上架完成: taskNo={}, 总数量={}", taskNo, totalPutawayQty);
        return taskMapper.selectByTaskNo(taskNo);
    }

    // ============================================================

    // 5. C3 合并上架（多SKU同托）
    // ============================================================

    /** 合并上架（多个SKU合并到同一个托盘，上架到同一个库位） */
    @Transactional(rollbackFor = Exception.class)
    public List<PutawayRecord> mergePutaway(
            String taskNo,
            List<String> detailNos,
            String targetLocation,
            String lpnNo,
            String operator) {
        log.info(
                "合并上架: taskNo={}, details={}, location={}, lpn={}",
                taskNo,
                detailNos,
                targetLocation,
                lpnNo);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("上架任务不存在: " + taskNo);

        List<PutawayRecord> records = new ArrayList<>();
        BigDecimal totalPutawayQty = BigDecimal.ZERO;

        for (String detailNo : detailNos) {
            PutawayTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
            if (detail == null) {
                log.warn("合并上架未找到明细: detailNo={}", detailNo);
                continue;
            }

            BigDecimal putawayQty = detail.getExpectedQty().subtract(detail.getPutawayQty());
            if (putawayQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 创建上架记录
            PutawayRecord record =
                    createPutawayRecord(
                            task,
                            detail,
                            putawayQty,
                            targetLocation,
                            PutawayType.MERGE.getCode(),
                            operator);
            record.setLpnNo(lpnNo);
            recordMapper.updateById(record);
            records.add(record);

            // 更新明细
            detail.setPutawayQty(detail.getExpectedQty());
            detail.setActualLocation(targetLocation);
            detail.setStatus(PutawayStatus.COMPLETED.getCode());
            taskDetailMapper.updateById(detail);

            totalPutawayQty = totalPutawayQty.add(putawayQty);
        }

        // 更新任务
        updateTaskAfterPutaway(task, totalPutawayQty);

        // 更新入库单
        updateInboundAfterPutaway(task, totalPutawayQty);

        log.info("合并上架完成: taskNo={}, 记录{}条, 总数量={}", taskNo, records.size(), totalPutawayQty);
        return records;
    }

    // ============================================================

    // 6. C4 批量上架（多托同库位）
    // ============================================================

    /** 批量上架（多个托盘上架到同一个库位，支持封存库位） */
    @Transactional(rollbackFor = Exception.class)
    public List<PutawayRecord> batchPutaway(
            String taskNo, String targetLocation, List<String> lpnNos, String operator) {
        log.info(
                "批量上架: taskNo={}, location={}, lpnCount={}",
                taskNo,
                targetLocation,
                lpnNos != null ? lpnNos.size() : 0);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("上架任务不存在: " + taskNo);

        List<PutawayTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        List<PutawayRecord> records = new ArrayList<>();
        BigDecimal totalPutawayQty = BigDecimal.ZERO;

        // 按LPN号匹配明细（简化处理，实际应通过LPN关联明细）
        for (PutawayTaskDetail detail : details) {
            if (PutawayStatus.COMPLETED.getCode().equals(detail.getStatus())) continue;

            BigDecimal putawayQty = detail.getExpectedQty().subtract(detail.getPutawayQty());
            if (putawayQty.compareTo(BigDecimal.ZERO) <= 0) continue;

            // 创建上架记录
            PutawayRecord record =
                    createPutawayRecord(
                            task,
                            detail,
                            putawayQty,
                            targetLocation,
                            PutawayType.BATCH.getCode(),
                            operator);
            if (lpnNos != null && !lpnNos.isEmpty()) {
                record.setLpnNo(lpnNos.get(0)); // 简化处理
            }
            recordMapper.updateById(record);
            records.add(record);

            // 更新明细
            detail.setPutawayQty(detail.getExpectedQty());
            detail.setActualLocation(targetLocation);
            detail.setStatus(PutawayStatus.COMPLETED.getCode());
            taskDetailMapper.updateById(detail);

            totalPutawayQty = totalPutawayQty.add(putawayQty);
        }

        // 更新任务
        updateTaskAfterPutaway(task, totalPutawayQty);

        // 更新入库单
        updateInboundAfterPutaway(task, totalPutawayQty);

        log.info("批量上架完成: taskNo={}, 记录{}条, 总数量={}", taskNo, records.size(), totalPutawayQty);
        return records;
    }

    // ============================================================

    // 7. C5 按箱码/LPN上架
    // ============================================================

    /** 按箱码/LPN上架 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRecord putawayByLpn(
            String taskNo,
            String lpnNo,
            String targetLocation,
            BigDecimal putawayQty,
            String operator) {
        log.info(
                "按LPN上架: taskNo={}, lpn={}, location={}, qty={}",
                taskNo,
                lpnNo,
                targetLocation,
                putawayQty);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("上架任务不存在: " + taskNo);

        // 通过LPN号匹配明细（简化处理，实际应查询LPN关联的明细）
        List<PutawayTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        PutawayTaskDetail matchedDetail = null;
        for (PutawayTaskDetail detail : details) {
            if (!PutawayStatus.COMPLETED.getCode().equals(detail.getStatus())) {
                matchedDetail = detail;
                break;
            }
        }

        if (matchedDetail == null) {
            throw new RuntimeException("未找到可上架的明细: lpnNo=" + lpnNo);
        }

        // 创建上架记录
        PutawayRecord record =
                createPutawayRecord(
                        task,
                        matchedDetail,
                        putawayQty,
                        targetLocation,
                        PutawayType.LPN.getCode(),
                        operator);
        record.setLpnNo(lpnNo);
        recordMapper.updateById(record);

        // 更新明细
        matchedDetail.setPutawayQty(matchedDetail.getPutawayQty().add(putawayQty));
        matchedDetail.setActualLocation(targetLocation);
        if (matchedDetail.getPutawayQty().compareTo(matchedDetail.getExpectedQty()) >= 0) {
            matchedDetail.setStatus(PutawayStatus.COMPLETED.getCode());
        } else {
            matchedDetail.setStatus(PutawayStatus.PUTAWAYING.getCode());
        }
        taskDetailMapper.updateById(matchedDetail);

        // 更新任务
        updateTaskAfterPutaway(task, putawayQty);

        // 更新入库单
        updateInboundAfterPutaway(task, putawayQty);

        log.info("按LPN上架完成: taskNo={}, lpn={}, qty={}", taskNo, lpnNo, putawayQty);
        return record;
    }

    // ============================================================

    // 8. C6 直接收货到存储库位（免上架）
    // ============================================================

    /** 直接收货到存储库位（收货时直接指定存储库位，免上架流程） */
    @Transactional(rollbackFor = Exception.class)
    public PutawayRecord directPutaway(
            String inboundNo,
            String inboundDetailNo,
            String targetLocation,
            BigDecimal putawayQty,
            String operator) {
        log.info(
                "直接收货到库位: inboundNo={}, detailNo={}, location={}, qty={}",
                inboundNo,
                inboundDetailNo,
                targetLocation,
                putawayQty);

        InboundOrder inboundOrder = inboundOrderMapper.selectByInboundNo(inboundNo);
        if (inboundOrder == null) throw new RuntimeException("入库单不存在: " + inboundNo);

        InboundDetail inboundDetail = inboundDetailMapper.selectByDetailNo(inboundDetailNo);
        if (inboundDetail == null) throw new RuntimeException("入库明细不存在: " + inboundDetailNo);

        // 创建上架任务（标记为直接上架）
        PutawayTask task = new PutawayTask();
        task.setTaskNo(generateTaskNo());
        task.setInboundNo(inboundNo);
        task.setAsnNo(inboundOrder.getAsnNo());
        task.setPutawayType(PutawayType.DIRECT.getCode());
        task.setWarehouseCode(inboundOrder.getWarehouseCode());
        task.setExpectedQty(putawayQty);
        task.setPutawayQty(putawayQty);
        task.setStatus(PutawayStatus.COMPLETED.getCode());
        task.setCompleteTime(LocalDateTime.now());
        task.setOperator(operator);
        task.setSource("DIRECT");
        task.setCreatedBy(operator);
        taskMapper.insert(task);

        // 创建上架任务明细
        PutawayTaskDetail detail = new PutawayTaskDetail();
        detail.setDetailNo(generateTaskDetailNo());
        detail.setTaskNo(task.getTaskNo());
        detail.setLineNo(1);
        detail.setInboundDetailNo(inboundDetailNo);
        detail.setSkuCode(inboundDetail.getSkuCode());
        detail.setSkuName(inboundDetail.getSkuName());
        detail.setExpectedQty(putawayQty);
        detail.setPutawayQty(putawayQty);
        detail.setActualLocation(targetLocation);
        detail.setStatus(PutawayStatus.COMPLETED.getCode());
        taskDetailMapper.insert(detail);

        // 创建上架记录
        PutawayRecord record = new PutawayRecord();
        record.setRecordNo(generateRecordNo());
        record.setTaskNo(task.getTaskNo());
        record.setInboundNo(inboundNo);
        record.setInboundDetailNo(inboundDetailNo);
        record.setSkuCode(inboundDetail.getSkuCode());
        record.setSkuName(inboundDetail.getSkuName());
        record.setPutawayQty(putawayQty);
        record.setTargetLocation(targetLocation);
        record.setPutawayType(PutawayType.DIRECT.getCode());
        record.setOperator(operator);
        record.setPutawayTime(LocalDateTime.now());
        recordMapper.insert(record);

        // 更新入库明细
        inboundDetail.setPutawayQty(inboundDetail.getPutawayQty().add(putawayQty));
        inboundDetail.setStatus("DONE");
        inboundDetailMapper.updateById(inboundDetail);

        // 更新入库单
        inboundOrder.setPutawayQty(inboundOrder.getPutawayQty().add(putawayQty));
        if (inboundOrder.getPutawayQty().compareTo(inboundOrder.getReceivedQty()) >= 0) {
            inboundOrder.setStatus("DONE");
            inboundOrder.setDoneTime(LocalDateTime.now());
        } else {
            inboundOrder.setStatus("PUTAWAYING");
        }
        inboundOrderMapper.updateById(inboundOrder);

        log.info(
                "直接收货到库位完成: inboundNo={}, location={}, qty={}",
                inboundNo,
                targetLocation,
                putawayQty);
        return record;
    }

    // ============================================================

    // 9. C7 码盘预约库位
    // ============================================================

    /** 码盘预约库位（收货前预计算上架库位，收货后直接上架） */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTaskDetail reservationPutaway(
            String taskNo, String detailNo, String reservedLocation, String operator) {
        log.info("码盘预约库位: taskNo={}, detailNo={}, location={}", taskNo, detailNo, reservedLocation);

        PutawayTaskDetail detail = taskDetailMapper.selectByDetailNo(detailNo);
        if (detail == null) throw new RuntimeException("上架任务明细不存在: " + detailNo);

        detail.setRecommendLocation(reservedLocation);
        detail.setUpdatedBy(operator);
        detail.setUpdatedTime(LocalDateTime.now());
        taskDetailMapper.updateById(detail);

        // 更新任务类型为预约上架
        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task != null) {
            task.setPutawayType(PutawayType.RESERVATION.getCode());
            taskMapper.updateById(task);
        }

        log.info("码盘预约库位完成: detailNo={}, location={}", detailNo, reservedLocation);
        return detail;
    }

    // ============================================================

    // 10. 上架完成
    // ============================================================

    /** 标记上架完成 */
    @Transactional(rollbackFor = Exception.class)
    public PutawayTask completePutaway(String taskNo, String operator) {
        log.info("标记上架完成: taskNo={}", taskNo);

        PutawayTask task = taskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("上架任务不存在: " + taskNo);

        task.setStatus(PutawayStatus.COMPLETED.getCode());
        task.setCompleteTime(LocalDateTime.now());
        task.setOperator(operator);
        taskMapper.updateById(task);

        // 更新所有明细状态
        List<PutawayTaskDetail> details = taskDetailMapper.selectByTaskNo(taskNo);
        for (PutawayTaskDetail detail : details) {
            if (!PutawayStatus.COMPLETED.getCode().equals(detail.getStatus())) {
                detail.setStatus(PutawayStatus.COMPLETED.getCode());
                taskDetailMapper.updateById(detail);
            }
        }

        // 更新入库单状态
        if (task.getInboundNo() != null) {
            InboundOrder inboundOrder = inboundOrderMapper.selectByInboundNo(task.getInboundNo());
            if (inboundOrder != null) {
                inboundOrder.setStatus("DONE");
                inboundOrder.setDoneTime(LocalDateTime.now());
                inboundOrderMapper.updateById(inboundOrder);
            }
        }

        log.info("标记上架完成: taskNo={}", taskNo);
        return task;
    }

    // ============================================================

    // 11. 公共方法
    // ============================================================

    /** 创建上架记录 */
    private PutawayRecord createPutawayRecord(
            PutawayTask task,
            PutawayTaskDetail detail,
            BigDecimal putawayQty,
            String targetLocation,
            String putawayType,
            String operator) {
        PutawayRecord record = new PutawayRecord();
        record.setRecordNo(generateRecordNo());
        record.setTaskNo(task.getTaskNo());
        record.setInboundNo(task.getInboundNo());
        record.setAsnNo(task.getAsnNo());
        record.setTaskDetailNo(detail.getDetailNo());
        record.setInboundDetailNo(detail.getInboundDetailNo());
        record.setSkuCode(detail.getSkuCode());
        record.setSkuName(detail.getSkuName());
        record.setBarcode(detail.getBarcode());
        record.setBatchNo(detail.getBatchNo());
        record.setPutawayQty(putawayQty);
        record.setUnit(detail.getUnit());
        record.setSourceLocation(detail.getSourceLocation());
        record.setRecommendLocation(detail.getRecommendLocation());
        record.setTargetLocation(targetLocation);
        record.setPutawayType(putawayType);
        record.setPutawayStrategy(task.getPutawayStrategy());
        record.setUseSystemRecommend(
                targetLocation != null && targetLocation.equals(detail.getRecommendLocation())
                        ? "Y"
                        : "N");
        record.setDifferenceQty(putawayQty.subtract(detail.getExpectedQty()));
        record.setDifferenceType(
                putawayQty.compareTo(detail.getExpectedQty()) > 0
                        ? "OVER"
                        : putawayQty.compareTo(detail.getExpectedQty()) < 0 ? "SHORT" : "NONE");
        record.setOperator(operator);
        record.setPutawayTime(LocalDateTime.now());
        recordMapper.insert(record);

        return record;
    }

    /** 上架后更新任务 */
    private void updateTaskAfterPutaway(PutawayTask task, BigDecimal putawayQty) {
        task.setPutawayQty(task.getPutawayQty().add(putawayQty));
        task.setDifferenceQty(task.getExpectedQty().subtract(task.getPutawayQty()));

        if (task.getPutawayQty().compareTo(BigDecimal.ZERO) == 0) {
            task.setStatus(PutawayStatus.PENDING.getCode());
        } else if (task.getPutawayQty().compareTo(task.getExpectedQty()) >= 0) {
            task.setStatus(PutawayStatus.COMPLETED.getCode());
            task.setCompleteTime(LocalDateTime.now());
        } else {
            task.setStatus(PutawayStatus.PARTIAL.getCode());
        }

        task.setOperator(task.getOperator());
        taskMapper.updateById(task);
    }

    /** 上架后更新入库单 */
    private void updateInboundAfterPutaway(PutawayTask task, BigDecimal putawayQty) {
        if (task.getInboundNo() == null) return;

        InboundOrder inboundOrder = inboundOrderMapper.selectByInboundNo(task.getInboundNo());
        if (inboundOrder == null) return;

        inboundOrder.setPutawayQty(inboundOrder.getPutawayQty().add(putawayQty));

        if (inboundOrder.getPutawayQty().compareTo(inboundOrder.getReceivedQty()) >= 0) {
            inboundOrder.setStatus("DONE");
            inboundOrder.setDoneTime(LocalDateTime.now());
        } else {
            inboundOrder.setStatus("PUTAWAYING");
        }

        inboundOrderMapper.updateById(inboundOrder);
    }

    // ============================================================

    // 12. 编号生成
    // ============================================================

    private String generateTaskNo() {
        return "PAT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskDetailNo() {
        return "PAD"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateRecordNo() {
        return "PAR"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
