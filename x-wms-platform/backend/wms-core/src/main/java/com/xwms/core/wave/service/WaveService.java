package com.xwms.core.wave.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.core.wave.entity.*;
import com.xwms.core.wave.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 波次管理核心服务 核心能力: 波次创建/波次分组/库存分配/拣货任务生成/路径规划/波次执行 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaveService {

    private final WaveMapper waveMapper;
    private final WaveDetailMapper waveDetailMapper;
    private final WavePickTaskMapper pickTaskMapper;
    private final WavePathMapper wavePathMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 波次创建与分组
    // ============================================================

    /** 创建波次（手动指定出库单） */
    @Transactional(rollbackFor = Exception.class)
    public Wave createWave(Wave wave, List<String> outboundNos, String operator) {
        if (wave.getWaveNo() == null) {
            wave.setWaveNo(generateWaveNo());
        }
        if (wave.getStatus() == null) wave.setStatus("CREATED");
        if (wave.getOrderCount() == null) wave.setOrderCount(0);
        if (wave.getSkuCount() == null) wave.setSkuCount(0);
        if (wave.getTotalQty() == null) wave.setTotalQty(BigDecimal.ZERO);
        if (wave.getPickedQty() == null) wave.setPickedQty(BigDecimal.ZERO);
        if (wave.getPriority() == null) wave.setPriority(5);
        wave.setCreatedBy(operator);
        waveMapper.insert(wave);

        // 添加出库单到波次
        if (outboundNos != null) {
            for (String outboundNo : outboundNos) {
                WaveDetail detail = new WaveDetail();
                detail.setWaveNo(wave.getWaveNo());
                detail.setOutboundNo(outboundNo);
                detail.setStatus("PENDING");
                detail.setPickedQty(BigDecimal.ZERO);
                waveDetailMapper.insert(detail);
            }
            wave.setOrderCount(outboundNos.size());
            waveMapper.updateById(wave);
        }

        log.info(
                "创建波次: {}, 出库单{}个", wave.getWaveNo(), outboundNos != null ? outboundNos.size() : 0);
        return wave;
    }

    /** 自动波次分组（根据波次策略） 分组维度: 仓库/承运商/配送区域/优先级/订单类型 */
    @Transactional(rollbackFor = Exception.class)
    public List<Wave> autoCreateWave(
            String warehouseCode,
            String ownerCode,
            String waveType,
            String pickMode,
            int maxOrderCount,
            BigDecimal maxTotalQty,
            List<Map<String, Object>> pendingOrders,
            String operator) {
        List<Wave> waves = new ArrayList<>();

        // 按承运商+配送区域分组
        Map<String, List<Map<String, Object>>> grouped =
                pendingOrders.stream()
                        .collect(
                                Collectors.groupingBy(
                                        o ->
                                                (o.get("carrier") != null
                                                                ? o.get("carrier").toString()
                                                                : "DEFAULT")
                                                        + "_"
                                                        + (o.get("area") != null
                                                                ? o.get("area").toString()
                                                                : "DEFAULT")));

        for (Map.Entry<String, List<Map<String, Object>>> entry : grouped.entrySet()) {
            List<Map<String, Object>> orders = entry.getValue();

            // 按最大订单数和最大数量拆分波次
            List<List<Map<String, Object>>> batches =
                    splitIntoBatches(orders, maxOrderCount, maxTotalQty);

            for (List<Map<String, Object>> batch : batches) {
                Wave wave = new Wave();
                wave.setWaveNo(generateWaveNo());
                wave.setWaveType(waveType);
                wave.setWarehouseCode(warehouseCode);
                wave.setOwnerCodeCol(ownerCode);
                wave.setPickMode(pickMode);
                wave.setStatus("CREATED");
                wave.setOrderCount(batch.size());
                wave.setTotalQty(
                        batch.stream()
                                .map(
                                        o ->
                                                new BigDecimal(
                                                        o.get("qty") != null
                                                                ? o.get("qty").toString()
                                                                : "0"))
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                wave.setPickedQty(BigDecimal.ZERO);
                wave.setPriority(5);
                wave.setCreatedBy(operator);
                waveMapper.insert(wave);

                // 添加明细
                for (Map<String, Object> order : batch) {
                    WaveDetail detail = new WaveDetail();
                    detail.setWaveNo(wave.getWaveNo());
                    detail.setOutboundNo(order.get("outboundNo").toString());
                    detail.setCustomerCode(
                            order.get("customerCode") != null
                                    ? order.get("customerCode").toString()
                                    : null);
                    detail.setCarrier(
                            order.get("carrier") != null ? order.get("carrier").toString() : null);
                    detail.setStatus("PENDING");
                    detail.setPickedQty(BigDecimal.ZERO);
                    waveDetailMapper.insert(detail);
                }

                waves.add(wave);
                log.info(
                        "自动创建波次: {}, 分组={}, 订单{}个", wave.getWaveNo(), entry.getKey(), batch.size());
            }
        }

        return waves;
    }

    /** 按最大订单数和最大数量拆分批次 */
    private List<List<Map<String, Object>>> splitIntoBatches(
            List<Map<String, Object>> orders, int maxOrderCount, BigDecimal maxTotalQty) {
        List<List<Map<String, Object>>> batches = new ArrayList<>();
        List<Map<String, Object>> current = new ArrayList<>();
        BigDecimal currentQty = BigDecimal.ZERO;

        for (Map<String, Object> order : orders) {
            BigDecimal qty =
                    new BigDecimal(order.get("qty") != null ? order.get("qty").toString() : "0");

            if (current.size() >= maxOrderCount || currentQty.add(qty).compareTo(maxTotalQty) > 0) {
                if (!current.isEmpty()) {
                    batches.add(new ArrayList<>(current));
                    current.clear();
                    currentQty = BigDecimal.ZERO;
                }
            }

            current.add(order);
            currentQty = currentQty.add(qty);
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        return batches;
    }

    // ============================================================

    // 2. 波次分配（库存预占）
    // ============================================================

    /** 执行波次分配 TODO: 调用分配规则引擎，根据周转规则和库位属性分配库存 */
    @Transactional(rollbackFor = Exception.class)
    public Wave allocateWave(String waveNo, String operator) {
        Wave wave = waveMapper.selectByWaveNo(waveNo);
        if (wave == null) throw new RuntimeException("波次不存在: " + waveNo);

        wave.setStatus("ALLOCATED");
        waveMapper.updateById(wave);

        // TODO: 对每个出库单执行库存分配
        // 这里简化处理，标记为已分配

        log.info("波次分配完成: {}", waveNo);
        return wave;
    }

    // ============================================================

    // 3. 拣货任务生成（按SKU+库位拆分）
    // ============================================================

    /**
     * 生成拣货任务 根据拣货模式生成不同的拣货任务: - PICK_BY_ORDER: 按出库单生成任务（摘果式） - PICK_BY_SKU: 按SKU+库位合并生成任务（播种式） -
     * PICK_BY_WAVE: 混合模式
     */
    @Transactional(rollbackFor = Exception.class)
    public List<WavePickTask> generatePickTasks(String waveNo, String pickMode) {
        Wave wave = waveMapper.selectByWaveNo(waveNo);
        if (wave == null) throw new RuntimeException("波次不存在: " + waveNo);

        List<WaveDetail> details = waveDetailMapper.selectByWaveNo(waveNo);
        List<WavePickTask> tasks = new ArrayList<>();

        if ("PICK_BY_SKU".equals(pickMode)) {
            // 播种式: 按SKU+库位合并
            Map<String, List<WaveDetail>> groupedBySku =
                    details.stream().collect(Collectors.groupingBy(WaveDetail::getOutboundNo));
            // TODO: 实际需要从出库明细获取SKU和库位信息
            // 这里简化处理
            int pathOrder = 1;
            for (WaveDetail detail : details) {
                WavePickTask task = new WavePickTask();
                task.setTaskNo(generateTaskNo());
                task.setWaveNo(waveNo);
                task.setSkuCode("SKU_" + detail.getOutboundNo()); // TODO: 实际SKU
                task.setFromLocation("LOC_DEFAULT"); // TODO: 实际库位
                task.setPickQty(BigDecimal.ONE); // TODO: 实际数量
                task.setPickedQty(BigDecimal.ZERO);
                task.setDifferenceQty(BigDecimal.ZERO);
                task.setStatus("PENDING");
                task.setPathOrder(pathOrder++);
                pickTaskMapper.insert(task);
                tasks.add(task);
            }
        } else {
            // 摘果式/混合: 按出库单生成任务
            int pathOrder = 1;
            for (WaveDetail detail : details) {
                WavePickTask task = new WavePickTask();
                task.setTaskNo(generateTaskNo());
                task.setWaveNo(waveNo);
                task.setSkuCode("SKU_" + detail.getOutboundNo());
                task.setFromLocation("LOC_DEFAULT");
                task.setPickQty(BigDecimal.ONE);
                task.setPickedQty(BigDecimal.ZERO);
                task.setDifferenceQty(BigDecimal.ZERO);
                task.setStatus("PENDING");
                task.setPathOrder(pathOrder++);
                pickTaskMapper.insert(task);
                tasks.add(task);
            }
        }

        wave.setSkuCount(tasks.size());
        waveMapper.updateById(wave);

        log.info("生成拣货任务: wave={}, 任务{}个, 模式={}", waveNo, tasks.size(), pickMode);
        return tasks;
    }

    // ============================================================

    // 4. 拣货路径规划（避免重走和拥堵）
    // ============================================================

    /** 规划拣货路径 算法: 最近邻算法(Nearest Neighbor) + S形路径 + 分区优化 目标: 最小化总行走距离，避免重走和拥堵 */
    @Transactional(rollbackFor = Exception.class)
    public List<WavePath> planPath(String waveNo) {
        List<WavePickTask> tasks = pickTaskMapper.selectByWaveNo(waveNo);
        if (tasks.isEmpty()) {
            throw new RuntimeException("波次无拣货任务: " + waveNo);
        }

        // TODO: 从库位管理获取库位坐标
        // 这里简化处理，使用模拟坐标
        List<WavePath> paths = new ArrayList<>();
        Random random = new Random(waveNo.hashCode());

        // 按库位分组
        Map<String, List<WavePickTask>> byLocation =
                tasks.stream().collect(Collectors.groupingBy(WavePickTask::getFromLocation));

        // 最近邻算法排序
        List<String> locationOrder =
                nearestNeighborSort(new ArrayList<>(byLocation.keySet()), random);

        int pathOrder = 1;
        BigDecimal prevX = BigDecimal.ZERO;
        BigDecimal prevY = BigDecimal.ZERO;

        for (String location : locationOrder) {
            List<WavePickTask> locTasks = byLocation.get(location);
            BigDecimal x = BigDecimal.valueOf(random.nextDouble() * 100);
            BigDecimal y = BigDecimal.valueOf(random.nextDouble() * 100);

            WavePath path = new WavePath();
            path.setWaveNo(waveNo);
            path.setPathOrder(pathOrder++);
            path.setLocationCode(location);
            path.setLocationX(x);
            path.setLocationY(y);
            path.setLocationZ(BigDecimal.ZERO);
            // 计算到下一库位距离（欧几里得距离）
            path.setDistance(
                    BigDecimal.valueOf(
                            Math.sqrt(
                                    Math.pow(x.doubleValue() - prevX.doubleValue(), 2)
                                            + Math.pow(y.doubleValue() - prevY.doubleValue(), 2))));
            path.setSkuCount(locTasks.size());
            path.setTotalQty(
                    locTasks.stream()
                            .map(WavePickTask::getPickQty)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            wavePathMapper.insert(path);
            paths.add(path);

            // 更新拣货任务的路径顺序
            for (WavePickTask task : locTasks) {
                task.setPathOrder(path.getPathOrder());
                pickTaskMapper.updateById(task);
            }

            prevX = x;
            prevY = y;
        }

        log.info(
                "规划拣货路径: wave={}, 库位{}个, 总距离={}",
                waveNo,
                paths.size(),
                paths.stream().map(WavePath::getDistance).reduce(BigDecimal.ZERO, BigDecimal::add));
        return paths;
    }

    /** 最近邻算法排序 */
    private List<String> nearestNeighborSort(List<String> locations, Random random) {
        if (locations.size() <= 1) return locations;

        List<String> result = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String current = locations.get(0);
        result.add(current);
        visited.add(current);

        while (visited.size() < locations.size()) {
            String nearest = null;
            double minDist = Double.MAX_VALUE;
            for (String loc : locations) {
                if (!visited.contains(loc)) {
                    double dist = random.nextDouble() * 100; // 模拟距离
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = loc;
                    }
                }
            }
            if (nearest != null) {
                result.add(nearest);
                visited.add(nearest);
                current = nearest;
            } else {
                break;
            }
        }

        return result;
    }

    // ============================================================

    // 5. 波次执行
    // ============================================================

    /** 开始波次拣货 */
    @Transactional(rollbackFor = Exception.class)
    public Wave startWave(String waveNo, String picker) {
        Wave wave = waveMapper.selectByWaveNo(waveNo);
        if (wave == null) throw new RuntimeException("波次不存在: " + waveNo);

        wave.setStatus("PICKING");
        wave.setAssignPicker(picker);
        wave.setStartTime(LocalDateTime.now());
        waveMapper.updateById(wave);

        log.info("开始波次拣货: {}, 拣货员={}", waveNo, picker);
        return wave;
    }

    /** 完成波次拣货 */
    @Transactional(rollbackFor = Exception.class)
    public Wave completeWave(String waveNo) {
        Wave wave = waveMapper.selectByWaveNo(waveNo);
        if (wave == null) throw new RuntimeException("波次不存在: " + waveNo);

        // 检查所有拣货任务是否完成
        List<WavePickTask> tasks = pickTaskMapper.selectByWaveNo(waveNo);
        boolean allDone = tasks.stream().allMatch(t -> "DONE".equals(t.getStatus()));
        if (!allDone) {
            throw new RuntimeException("波次尚有未完成的拣货任务: " + waveNo);
        }

        wave.setStatus("PICKED");
        wave.setEndTime(LocalDateTime.now());
        wave.setPickedQty(
                tasks.stream()
                        .map(WavePickTask::getPickedQty)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        waveMapper.updateById(wave);

        log.info("波次拣货完成: {}, 拣货数量={}", waveNo, wave.getPickedQty());
        return wave;
    }

    /** 执行单个拣货任务 */
    @Transactional(rollbackFor = Exception.class)
    public WavePickTask executePickTask(
            String taskNo, BigDecimal pickedQty, BigDecimal differenceQty, String picker) {
        WavePickTask task = pickTaskMapper.selectByTaskNo(taskNo);
        if (task == null) throw new RuntimeException("拣货任务不存在: " + taskNo);

        task.setPickedQty(pickedQty);
        task.setDifferenceQty(differenceQty != null ? differenceQty : BigDecimal.ZERO);
        task.setPicker(picker);
        task.setStatus("DONE");
        task.setPickTime(LocalDateTime.now());
        pickTaskMapper.updateById(task);

        // 更新波次明细状态
        Wave wave = waveMapper.selectByWaveNo(task.getWaveNo());
        if (wave != null) {
            wave.setPickedQty(wave.getPickedQty().add(pickedQty));
            waveMapper.updateById(wave);
        }

        log.info("拣货任务完成: task={}, picked={}, diff={}", taskNo, pickedQty, differenceQty);
        return task;
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public Page<Wave> pageWaves(
            Page<Wave> page,
            String waveType,
            String status,
            String warehouseCode,
            String ownerCode) {
        LambdaQueryWrapper<Wave> wrapper = new LambdaQueryWrapper<>();
        if (waveType != null) wrapper.eq(Wave::getWaveType, waveType);
        if (status != null) wrapper.eq(Wave::getStatus, status);
        if (warehouseCode != null) wrapper.eq(Wave::getWarehouseCode, warehouseCode);
        if (ownerCode != null) wrapper.eq(Wave::getOwnerCodeCol, ownerCode);
        wrapper.orderByDesc(Wave::getCreatedTime);
        return waveMapper.selectPage(page, wrapper);
    }

    public Wave getWaveByNo(String waveNo) {
        return waveMapper.selectByWaveNo(waveNo);
    }

    public List<WaveDetail> getWaveDetails(String waveNo) {
        return waveDetailMapper.selectByWaveNo(waveNo);
    }

    public List<WavePickTask> getPickTasks(String waveNo) {
        return pickTaskMapper.selectByWaveNo(waveNo);
    }

    public List<WavePath> getWavePaths(String waveNo) {
        return wavePathMapper.selectByWaveNo(waveNo);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateWaveNo() {
        return "WAVE"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateTaskNo() {
        return "TASK"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
