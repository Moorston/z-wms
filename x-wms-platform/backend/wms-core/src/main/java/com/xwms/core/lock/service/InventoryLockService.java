package com.xwms.core.lock.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.xwms.core.lock.entity.*;
import com.xwms.core.lock.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 库存锁管理核心服务 核心能力: 锁规则/锁申请/锁释放/锁等待/死锁检测
 *
 * <p>锁兼容性矩阵: SHARED EXCLUSIVE SHARED 兼容 冲突 EXCLUSIVE 冲突 冲突
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryLockService {

    private final LockRuleMapper ruleMapper;
    private final InventoryLockMapper lockMapper;
    private final LockWaitQueueMapper waitQueueMapper;
    private final DeadlockLogMapper deadlockLogMapper;
    private final ObjectMapper objectMapper;

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 锁规则管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public LockRule createRule(LockRule rule) {
        rule.setStatus("ACTIVE");
        if (rule.getMaxWaitTime() == null) rule.setMaxWaitTime(30);
        if (rule.getTimeoutTime() == null) rule.setTimeoutTime(300);
        if (rule.getRetryCount() == null) rule.setRetryCount(3);
        if (rule.getRetryInterval() == null) rule.setRetryInterval(1000);
        if (rule.getDeadlockDetect() == null) rule.setDeadlockDetect("Y");
        ruleMapper.insert(rule);
        log.info(
                "创建库存锁规则: {}={}, scope={}, type={}",
                rule.getRuleCode(),
                rule.getRuleName(),
                rule.getLockScope(),
                rule.getLockType());
        return rule;
    }

    public Page<LockRule> pageRules(Page<LockRule> page) {
        LambdaQueryWrapper<LockRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LockRule::getStatus, "ACTIVE");
        return ruleMapper.selectPage(page, wrapper);
    }

    public LockRule getRuleByCode(String ruleCode) {
        return ruleMapper.selectByRuleCode(ruleCode);
    }

    // ============================================================

    // 2. 锁申请（核心）
    // ============================================================

    /**
     * 申请库存锁
     *
     * @param lockKey 锁键
     * @param lockScope 锁范围
     * @param lockType 锁类型: SHARED/EXCLUSIVE
     * @param businessType 业务类型
     * @param businessNo 业务单号
     * @param holder 持有者
     * @param lockQuantity 锁定数量
     * @return 锁记录(成功返回HELD，失败返回WAITING或抛出异常)
     */
    @Transactional(rollbackFor = Exception.class)
    public InventoryLock acquireLock(
            String lockKey,
            String lockScope,
            String lockType,
            String businessType,
            String businessNo,
            String holder,
            BigDecimal lockQuantity) {
        // 检查是否有冲突的锁
        boolean conflict = checkConflict(lockKey, lockType);

        if (conflict) {
            // 加入等待队列
            return waitForLock(
                    lockKey, lockScope, lockType, businessType, businessNo, holder, lockQuantity);
        }

        // 无冲突，直接获取锁
        return doAcquireLock(
                lockKey, lockScope, lockType, businessType, businessNo, holder, lockQuantity);
    }

    /** 检查锁冲突 兼容性: - SHARED vs SHARED: 兼容 - SHARED vs EXCLUSIVE: 冲突 - EXCLUSIVE vs 任何: 冲突 */
    private boolean checkConflict(String lockKey, String requestType) {
        List<InventoryLock> heldLocks = lockMapper.selectHeldByLockKey(lockKey);
        if (heldLocks.isEmpty()) return false;

        for (InventoryLock held : heldLocks) {
            if ("EXCLUSIVE".equals(held.getLockType())) {
                // 持有排他锁，任何请求都冲突
                return true;
            }
            if ("EXCLUSIVE".equals(requestType)) {
                // 请求排他锁，持有共享锁也冲突
                return true;
            }
        }
        return false;
    }

    /** 执行获取锁 */
    private InventoryLock doAcquireLock(
            String lockKey,
            String lockScope,
            String lockType,
            String businessType,
            String businessNo,
            String holder,
            BigDecimal lockQuantity) {
        LockRule rule = ruleMapper.matchRule(lockScope, lockType);
        int timeoutSeconds = rule != null ? rule.getTimeoutTime() : 300;

        InventoryLock lock = new InventoryLock();
        lock.setLockId(generateLockId());
        lock.setLockKey(lockKey);
        lock.setLockScope(lockScope);
        lock.setLockType(lockType);
        lock.setBusinessType(businessType);
        lock.setBusinessNo(businessNo);
        lock.setHolder(holder);
        lock.setLockQuantity(lockQuantity);
        lock.setStatus("HELD");
        lock.setAcquireTime(LocalDateTime.now());
        lock.setExpireTime(LocalDateTime.now().plusSeconds(timeoutSeconds));
        lockMapper.insert(lock);

        log.info(
                "获取库存锁: lockId={}, key={}, type={}, holder={}, business={}",
                lock.getLockId(),
                lockKey,
                lockType,
                holder,
                businessNo);
        return lock;
    }

    /** 加入等待队列 */
    private InventoryLock waitForLock(
            String lockKey,
            String lockScope,
            String lockType,
            String businessType,
            String businessNo,
            String holder,
            BigDecimal lockQuantity) {
        LockRule rule = ruleMapper.matchRule(lockScope, lockType);
        int maxWaitSeconds = rule != null ? rule.getMaxWaitTime() : 30;

        LockWaitQueue wait = new LockWaitQueue();
        wait.setWaitId(generateWaitId());
        wait.setLockKey(lockKey);
        wait.setLockType(lockType);
        wait.setRequester(holder);
        wait.setBusinessNo(businessNo);
        wait.setWaitStartTime(LocalDateTime.now());
        wait.setWaitTimeout(LocalDateTime.now().plusSeconds(maxWaitSeconds));
        wait.setStatus("WAITING");
        wait.setPriority(5);
        waitQueueMapper.insert(wait);

        log.info(
                "加入锁等待队列: waitId={}, key={}, type={}, requester={}",
                wait.getWaitId(),
                lockKey,
                lockType,
                holder);

        // 返回等待状态的锁记录
        InventoryLock lock = new InventoryLock();
        lock.setLockId(wait.getWaitId());
        lock.setLockKey(lockKey);
        lock.setLockScope(lockScope);
        lock.setLockType(lockType);
        lock.setBusinessType(businessType);
        lock.setBusinessNo(businessNo);
        lock.setHolder(holder);
        lock.setLockQuantity(lockQuantity);
        lock.setStatus("WAITING");
        return lock;
    }

    // ============================================================

    // 3. 锁释放
    // ============================================================

    /** 释放库存锁 */
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseLock(String lockId) {
        InventoryLock lock = lockMapper.selectByLockId(lockId);
        if (lock == null) {
            log.warn("释放锁失败: 锁不存在 lockId={}", lockId);
            return false;
        }
        if (!"HELD".equals(lock.getStatus())) {
            log.warn("释放锁失败: 锁状态不正确 lockId={}, status={}", lockId, lock.getStatus());
            return false;
        }

        lockMapper.updateStatus(lockId, "RELEASED");
        log.info(
                "释放库存锁: lockId={}, key={}, holder={}", lockId, lock.getLockKey(), lock.getHolder());

        // 唤醒等待队列中的下一个请求
        wakeUpNextWaiter(lock.getLockKey());

        return true;
    }

    /** 释放业务单关联的所有锁 */
    @Transactional(rollbackFor = Exception.class)
    public int releaseLocksByBusinessNo(String businessNo) {
        List<InventoryLock> locks = lockMapper.selectHeldByBusinessNo(businessNo);
        int count = 0;
        for (InventoryLock lock : locks) {
            if (releaseLock(lock.getLockId())) {
                count++;
            }
        }
        log.info("释放业务单关联锁: businessNo={}, count={}", businessNo, count);
        return count;
    }

    /** 唤醒等待队列中的下一个请求 */
    private void wakeUpNextWaiter(String lockKey) {
        List<LockWaitQueue> waiters = waitQueueMapper.selectWaitingByLockKey(lockKey);
        if (waiters.isEmpty()) return;

        // 取优先级最高、等待最久的
        LockWaitQueue next = waiters.get(0);

        // 检查是否仍有冲突
        boolean conflict = checkConflict(lockKey, next.getLockType());
        if (conflict) {
            log.info("锁仍被占用，等待下一次唤醒: key={}, waitId={}", lockKey, next.getWaitId());
            return;
        }

        // 获取锁
        InventoryLock lock =
                doAcquireLock(
                        lockKey,
                        next.getLockType(),
                        next.getLockType(),
                        null,
                        next.getBusinessNo(),
                        next.getRequester(),
                        null);

        // 更新等待队列状态
        waitQueueMapper.updateStatus(next.getWaitId(), "ACQUIRED");

        log.info(
                "唤醒等待请求并获取锁: waitId={}, lockId={}, key={}",
                next.getWaitId(),
                lock.getLockId(),
                lockKey);
    }

    // ============================================================

    // 4. 死锁检测
    // ============================================================

    /** 执行死锁检测 原理: 构建等待图，检测是否存在环 */
    @Transactional(rollbackFor = Exception.class)
    public List<DeadlockLog> detectDeadlocks() {
        List<DeadlockLog> results = new ArrayList<>();

        // 获取所有等待中的请求
        List<LockWaitQueue> allWaiting =
                waitQueueMapper.selectList(
                        new LambdaQueryWrapper<LockWaitQueue>()
                                .eq(LockWaitQueue::getStatus, "WAITING"));

        // 构建等待图: holder -> 等待的lockKey -> 持有该lockKey的holder
        Map<String, Set<String>> waitGraph = new HashMap<>();
        Map<String, String> waitLockKeyMap = new HashMap<>();

        for (LockWaitQueue wait : allWaiting) {
            String requester = wait.getRequester();
            String lockKey = wait.getLockKey();
            waitLockKeyMap.put(requester, lockKey);

            // 找到持有该锁的holder
            List<InventoryLock> holders = lockMapper.selectHeldByLockKey(lockKey);
            for (InventoryLock holder : holders) {
                waitGraph.computeIfAbsent(requester, k -> new HashSet<>()).add(holder.getHolder());
            }
        }

        // 检测环
        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (String node : waitGraph.keySet()) {
            if (detectCycle(node, waitGraph, visited, recStack)) {
                // 发现死锁，记录并解决
                DeadlockLog deadlock = resolveDeadlock(node, waitGraph, waitLockKeyMap);
                results.add(deadlock);
            }
        }

        log.info("死锁检测完成: 发现 {} 个死锁", results.size());
        return results;
    }

    /** DFS检测环 */
    private boolean detectCycle(
            String node,
            Map<String, Set<String>> graph,
            Set<String> visited,
            Set<String> recStack) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recStack.add(node);

        for (String neighbor : graph.getOrDefault(node, Collections.emptySet())) {
            if (detectCycle(neighbor, graph, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(node);
        return false;
    }

    /** 解决死锁: 牺牲一个锁 */
    private DeadlockLog resolveDeadlock(
            String cycleNode,
            Map<String, Set<String>> waitGraph,
            Map<String, String> waitLockKeyMap) {
        // 找到环中所有节点
        Set<String> cycle = new HashSet<>();
        collectCycle(cycleNode, waitGraph, cycle, new HashSet<>());

        // 选择牺牲者: 持有锁最多的或等待最久的
        String victim = cycle.iterator().next();
        String victimLockKey = waitLockKeyMap.get(victim);

        // 找到受害者持有的锁
        List<InventoryLock> victimLocks = lockMapper.selectHeldByHolder(victim);
        String victimLockId = victimLocks.isEmpty() ? null : victimLocks.get(0).getLockId();

        // 强制释放受害者的锁
        if (victimLockId != null) {
            lockMapper.updateStatus(victimLockId, "DEADLOCK");
        }

        // 记录死锁日志
        DeadlockLog log = new DeadlockLog();
        log.setLogId(generateLogId());
        log.setDetectTime(LocalDateTime.now());
        log.setLockKey(victimLockKey);
        try {
            log.setInvolvedLocks(objectMapper.writeValueAsString(cycle));
            log.setInvolvedHolders(objectMapper.writeValueAsString(waitGraph));
        } catch (JsonProcessingException e) {
            log.setInvolvedLocks(cycle.toString());
        }
        log.setVictimLockId(victimLockId);
        log.setVictimHolder(victim);
        log.setResolveAction("KILL_VICTIM");
        log.setResolveResult("SUCCESS");
        log.setResolveTime(LocalDateTime.now());
        deadlockLogMapper.insert(log);

        // 唤醒等待者
        if (victimLockKey != null) {
            wakeUpNextWaiter(victimLockKey);
        }

        return log;
    }

    private void collectCycle(
            String node, Map<String, Set<String>> graph, Set<String> cycle, Set<String> visited) {
        if (visited.contains(node)) return;
        visited.add(node);
        cycle.add(node);
        for (String neighbor : graph.getOrDefault(node, Collections.emptySet())) {
            collectCycle(neighbor, graph, cycle, visited);
        }
    }

    // ============================================================

    // 5. 超时清理
    // ============================================================

    /** 清理超时的锁和等待 */
    @Transactional(rollbackFor = Exception.class)
    public int cleanupTimeout() {
        int lockCount = lockMapper.timeoutExpiredLocks();
        int waitCount = waitQueueMapper.timeoutExpiredWaits();
        log.info("清理超时: 锁={}, 等待={}", lockCount, waitCount);
        return lockCount + waitCount;
    }

    // ============================================================

    // 6. 查询
    // ============================================================

    public InventoryLock getLockById(String lockId) {
        return lockMapper.selectByLockId(lockId);
    }

    public List<InventoryLock> getHeldLocksByLockKey(String lockKey) {
        return lockMapper.selectHeldByLockKey(lockKey);
    }

    public List<InventoryLock> getHeldLocksByBusinessNo(String businessNo) {
        return lockMapper.selectHeldByBusinessNo(businessNo);
    }

    public List<InventoryLock> getHeldLocksByHolder(String holder) {
        return lockMapper.selectHeldByHolder(holder);
    }

    public Page<InventoryLock> pageLocks(Page<InventoryLock> page, String status) {
        LambdaQueryWrapper<InventoryLock> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(InventoryLock::getStatus, status);
        wrapper.orderByDesc(InventoryLock::getAcquireTime);
        return lockMapper.selectPage(page, wrapper);
    }

    public Page<LockWaitQueue> pageWaitQueue(Page<LockWaitQueue> page, String status) {
        LambdaQueryWrapper<LockWaitQueue> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(LockWaitQueue::getStatus, status);
        wrapper.orderByDesc(LockWaitQueue::getWaitStartTime);
        return waitQueueMapper.selectPage(page, wrapper);
    }

    public List<DeadlockLog> getRecentDeadlocks(int limit) {
        return deadlockLogMapper.selectRecent(limit);
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private String generateLockId() {
        return "LK"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateWaitId() {
        return "WT"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }

    private String generateLogId() {
        return "DL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", SEQ.incrementAndGet() % 1000);
    }
}
