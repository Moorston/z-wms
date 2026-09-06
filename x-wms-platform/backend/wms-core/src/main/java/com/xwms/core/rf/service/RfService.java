package com.xwms.core.rf.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.xwms.common.exception.BizException;
import com.xwms.core.rf.entity.*;
import com.xwms.core.rf.enums.RfTaskStatus;
import com.xwms.core.rf.mapper.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** RF作业管理核心服务 包含: RF任务/RF会话/RF作业记录/RF菜单 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RfService {

    private final RfTaskMapper rfTaskMapper;
    private final RfWorkLogMapper workLogMapper;
    private final RfSessionMapper sessionMapper;
    private final RfMenuMapper menuMapper;

    private static final AtomicInteger TASK_SEQ = new AtomicInteger(0);
    private static final AtomicInteger LOG_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. RF任务管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public RfTask createTask(RfTask task) {
        task.setTaskNo(generateTaskNo());
        task.setStatus(RfTaskStatus.PENDING.getCode());
        task.setQuantityDone(BigDecimal.ZERO);
        rfTaskMapper.insert(task);
        log.info("创建RF任务: {}, 类型={}", task.getTaskNo(), task.getTaskType());
        return task;
    }

    public RfTask getTask(Long id) {
        RfTask task = rfTaskMapper.selectById(id);
        if (task == null) throw new BizException("RF任务不存在: " + id);
        return task;
    }

    public Page<RfTask> pageTasks(Page<RfTask> page, String type, String status, String warehouse) {
        LambdaQueryWrapper<RfTask> wrapper = new LambdaQueryWrapper<>();
        if (type != null) wrapper.eq(RfTask::getTaskType, type);
        if (status != null) wrapper.eq(RfTask::getStatus, status);
        if (warehouse != null) wrapper.eq(RfTask::getWarehouseCode, warehouse);
        wrapper.orderByDesc(RfTask::getCreatedTime);
        return rfTaskMapper.selectPage(page, wrapper);
    }

    /** 领取任务 */
    @Transactional(rollbackFor = Exception.class)
    public RfTask acceptTask(Long taskId, String userId, String userName, String pdaDeviceId) {
        RfTask task = getTask(taskId);
        if (!RfTaskStatus.PENDING.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许领取: " + task.getStatus());
        }
        task.setStatus(RfTaskStatus.ASSIGNED.getCode());
        task.setAssignedTo(userId);
        task.setAssignedName(userName);
        task.setPdaDeviceId(pdaDeviceId);
        rfTaskMapper.updateById(task);

        writeLog(task, userId, userName, pdaDeviceId, "ACCEPT", null, null, null, null, null, null);
        log.info("领取RF任务: {}, 用户={}", task.getTaskNo(), userName);
        return task;
    }

    /** 开始任务 */
    @Transactional(rollbackFor = Exception.class)
    public RfTask startTask(Long taskId, String userId, String userName, String pdaDeviceId) {
        RfTask task = getTask(taskId);
        if (!RfTaskStatus.ASSIGNED.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许开始: " + task.getStatus());
        }
        task.setStatus(RfTaskStatus.IN_PROGRESS.getCode());
        task.setStartTime(LocalDateTime.now());
        rfTaskMapper.updateById(task);

        writeLog(task, userId, userName, pdaDeviceId, "START", null, null, null, null, null, null);
        log.info("开始RF任务: {}", task.getTaskNo());
        return task;
    }

    /** 扫码确认 */
    @Transactional(rollbackFor = Exception.class)
    public RfTask scanConfirm(
            Long taskId,
            String userId,
            String userName,
            String pdaDeviceId,
            String location,
            String barcode,
            String sku,
            String batchNo,
            BigDecimal quantity) {
        RfTask task = getTask(taskId);
        if (!RfTaskStatus.IN_PROGRESS.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许操作: " + task.getStatus());
        }

        // 累加已完成数量
        if (quantity != null) {
            task.setQuantityDone(task.getQuantityDone().add(quantity));
        }
        rfTaskMapper.updateById(task);

        writeLog(
                task,
                userId,
                userName,
                pdaDeviceId,
                "SCAN",
                task.getLocationFrom(),
                location,
                sku,
                batchNo,
                barcode,
                quantity);
        log.info("RF扫码: 任务={}, 库位={}, 条码={}, 数量={}", task.getTaskNo(), location, barcode, quantity);
        return task;
    }

    /** 完成任务 */
    @Transactional(rollbackFor = Exception.class)
    public RfTask completeTask(Long taskId, String userId, String userName, String pdaDeviceId) {
        RfTask task = getTask(taskId);
        if (!RfTaskStatus.IN_PROGRESS.getCode().equals(task.getStatus())
                && !RfTaskStatus.PAUSED.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许完成: " + task.getStatus());
        }
        task.setStatus(RfTaskStatus.COMPLETED.getCode());
        task.setEndTime(LocalDateTime.now());
        if (task.getStartTime() != null) {
            long minutes =
                    java.time.Duration.between(task.getStartTime(), LocalDateTime.now())
                            .toMinutes();
            task.setDurationMin((int) minutes);
        }
        rfTaskMapper.updateById(task);

        writeLog(
                task,
                userId,
                userName,
                pdaDeviceId,
                "COMPLETE",
                null,
                null,
                null,
                null,
                null,
                null);
        log.info("完成RF任务: {}, 耗时={}分钟", task.getTaskNo(), task.getDurationMin());
        return task;
    }

    /** 暂停任务 */
    @Transactional(rollbackFor = Exception.class)
    public RfTask pauseTask(Long taskId, String userId, String userName, String pdaDeviceId) {
        RfTask task = getTask(taskId);
        if (!RfTaskStatus.IN_PROGRESS.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许暂停: " + task.getStatus());
        }
        task.setStatus(RfTaskStatus.PAUSED.getCode());
        rfTaskMapper.updateById(task);
        writeLog(task, userId, userName, pdaDeviceId, "PAUSE", null, null, null, null, null, null);
        log.info("暂停RF任务: {}", task.getTaskNo());
        return task;
    }

    /** 继续任务 */
    @Transactional(rollbackFor = Exception.class)
    public RfTask resumeTask(Long taskId, String userId, String userName, String pdaDeviceId) {
        RfTask task = getTask(taskId);
        if (!RfTaskStatus.PAUSED.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许继续: " + task.getStatus());
        }
        task.setStatus(RfTaskStatus.IN_PROGRESS.getCode());
        rfTaskMapper.updateById(task);
        writeLog(task, userId, userName, pdaDeviceId, "RESUME", null, null, null, null, null, null);
        log.info("继续RF任务: {}", task.getTaskNo());
        return task;
    }

    /** 取消任务 */
    @Transactional(rollbackFor = Exception.class)
    public RfTask cancelTask(
            Long taskId, String userId, String userName, String pdaDeviceId, String remark) {
        RfTask task = getTask(taskId);
        if (RfTaskStatus.COMPLETED.getCode().equals(task.getStatus())
                || RfTaskStatus.CANCELLED.getCode().equals(task.getStatus())) {
            throw new BizException("任务状态不允许取消: " + task.getStatus());
        }
        task.setStatus(RfTaskStatus.CANCELLED.getCode());
        task.setRemark(remark);
        rfTaskMapper.updateById(task);
        writeLog(task, userId, userName, pdaDeviceId, "CANCEL", null, null, null, null, null, null);
        log.info("取消RF任务: {}", task.getTaskNo());
        return task;
    }

    /** 查询待领取任务 */
    public List<RfTask> getPendingTasks(String type, String warehouse) {
        return rfTaskMapper.selectPendingByType(type, warehouse);
    }

    /** 查询我的任务 */
    public List<RfTask> getMyTasks(String userId) {
        return rfTaskMapper.selectMyTasks(userId);
    }

    // ============================================================

    // 2. RF会话管理
    // ============================================================

    @Transactional(rollbackFor = Exception.class)
    public RfSession login(
            String userId,
            String userName,
            String pdaDeviceId,
            String warehouse,
            String ipAddress) {
        // 检查是否已有活跃会话
        RfSession existing = sessionMapper.selectActiveByUserId(userId);
        if (existing != null) {
            existing.setStatus("LOGOUT");
            existing.setLogoutTime(LocalDateTime.now());
            sessionMapper.updateById(existing);
        }

        RfSession session = new RfSession();
        session.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        session.setUserId(userId);
        session.setUserName(userName);
        session.setPdaDeviceId(pdaDeviceId);
        session.setWarehouseCode(warehouse);
        session.setLoginTime(LocalDateTime.now());
        session.setLastActiveTime(LocalDateTime.now());
        session.setStatus("ACTIVE");
        session.setIpAddress(ipAddress);
        sessionMapper.insert(session);

        log.info("RF登录: 用户={}, PDA={}, 会话={}", userName, pdaDeviceId, session.getSessionId());
        return session;
    }

    @Transactional(rollbackFor = Exception.class)
    public void logout(String sessionId) {
        RfSession session = sessionMapper.selectBySessionId(sessionId);
        if (session == null) throw new BizException("会话不存在");
        session.setStatus("LOGOUT");
        session.setLogoutTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        log.info("RF登出: 会话={}", sessionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void heartbeat(String sessionId) {
        RfSession session = sessionMapper.selectBySessionId(sessionId);
        if (session != null) {
            session.setLastActiveTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    public RfSession getSession(String sessionId) {
        return sessionMapper.selectBySessionId(sessionId);
    }

    // ============================================================

    // 3. RF作业记录
    // ============================================================

    public List<RfWorkLog> getTaskLogs(Long taskId) {
        return workLogMapper.selectByTaskId(taskId);
    }

    public List<RfWorkLog> getUserLogs(String userId) {
        return workLogMapper.selectByUserId(userId);
    }

    // ============================================================

    // 4. RF菜单管理
    // ============================================================

    public List<RfMenu> getAllMenus() {
        return menuMapper.selectAllEnabled();
    }

    public List<RfMenu> getChildMenus(String parentCode) {
        return menuMapper.selectByParent(parentCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public RfMenu createMenu(RfMenu menu) {
        menu.setEnabled(1);
        menuMapper.insert(menu);
        return menu;
    }

    // ============================================================

    // 工具方法
    // ============================================================

    private void writeLog(
            RfTask task,
            String userId,
            String userName,
            String pdaDeviceId,
            String action,
            String locationFrom,
            String locationTo,
            String sku,
            String batchNo,
            String barcode,
            BigDecimal quantity) {
        RfWorkLog logEntry = new RfWorkLog();
        logEntry.setLogNo(generateLogNo());
        logEntry.setTaskId(task.getId());
        logEntry.setTaskNo(task.getTaskNo());
        logEntry.setTaskType(task.getTaskType());
        logEntry.setUserId(userId);
        logEntry.setUserName(userName);
        logEntry.setPdaDeviceId(pdaDeviceId);
        logEntry.setAction(action);
        logEntry.setLocationFrom(locationFrom);
        logEntry.setLocationTo(locationTo);
        logEntry.setSku(sku);
        logEntry.setBatchNo(batchNo);
        logEntry.setBarcode(barcode);
        logEntry.setQuantity(quantity);
        logEntry.setBeforeStatus(task.getStatus());
        logEntry.setOwnerCodeCol(task.getOwnerCodeCol());
        logEntry.setWarehouseCodeCol(task.getWarehouseCodeCol());
        workLogMapper.insert(logEntry);
    }

    private String generateTaskNo() {
        return "RF"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", TASK_SEQ.incrementAndGet() % 1000);
    }

    private String generateLogNo() {
        return "RL"
                + LocalDateTime.now().format(NO_FMT)
                + String.format("%03d", LOG_SEQ.incrementAndGet() % 1000);
    }
}
