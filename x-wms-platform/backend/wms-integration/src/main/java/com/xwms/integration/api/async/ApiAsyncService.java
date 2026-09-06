package com.xwms.integration.api.async;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 异步API服务
 *
 * <p>核心能力： 1. 异步任务状态管理（ACCEPTED/PROCESSING/SUCCESS/FAILED） 2. 结果存储（Redis，24小时有效） 3. 结果查询接口 4.
 * 回调通知（处理完成后回调外部系统）
 *
 * <p>适用场景：批量导入、报表生成、大规模数据同步等耗时操作
 */
@Slf4j
@Service
public class ApiAsyncService {

    private final RedisTemplate<String, Object> redisTemplate;

    /** requestId -> 任务状态（内存索引，Redis持久化） */
    private final Map<String, AsyncTask> taskCache = new ConcurrentHashMap<>();

    private static final String REDIS_PREFIX = "api:async:";
    private static final long TTL_HOURS = 24;

    public ApiAsyncService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Data
    public static class AsyncTask {
        private String requestId;
        private String apiId;
        private String status; // ACCEPTED/PROCESSING/SUCCESS/FAILED
        private Object request;
        private Object result;
        private String error;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
        private String callbackUrl; // 回调地址
    }

    /** 创建异步任务 */
    public AsyncTask createTask(
            String requestId, String apiId, Object request, String callbackUrl) {
        AsyncTask task = new AsyncTask();
        task.setRequestId(requestId);
        task.setApiId(apiId);
        task.setStatus("ACCEPTED");
        task.setRequest(request);
        task.setCallbackUrl(callbackUrl);
        task.setCreatedAt(LocalDateTime.now());
        taskCache.put(requestId, task);
        saveToRedis(task);
        log.info("创建异步任务: requestId={}, apiId={}", requestId, apiId);
        return task;
    }

    /** 更新任务状态为处理中 */
    public void markProcessing(String requestId) {
        AsyncTask task = taskCache.get(requestId);
        if (task != null) {
            task.setStatus("PROCESSING");
            saveToRedis(task);
        }
    }

    /** 标记任务成功 */
    public void markSuccess(String requestId, Object result) {
        AsyncTask task = taskCache.get(requestId);
        if (task != null) {
            task.setStatus("SUCCESS");
            task.setResult(result);
            task.setCompletedAt(LocalDateTime.now());
            saveToRedis(task);
            log.info("异步任务成功: requestId={}", requestId);
            // 触发回调
            if (task.getCallbackUrl() != null) {
                triggerCallback(task);
            }
        }
    }

    /** 标记任务失败 */
    public void markFailed(String requestId, String error) {
        AsyncTask task = taskCache.get(requestId);
        if (task != null) {
            task.setStatus("FAILED");
            task.setError(error);
            task.setCompletedAt(LocalDateTime.now());
            saveToRedis(task);
            log.error("异步任务失败: requestId={}, error={}", requestId, error);
            if (task.getCallbackUrl() != null) {
                triggerCallback(task);
            }
        }
    }

    /** 查询任务状态 */
    public AsyncTask getTask(String requestId) {
        // 先查内存，再查Redis
        AsyncTask task = taskCache.get(requestId);
        if (task == null) {
            task = (AsyncTask) redisTemplate.opsForValue().get(REDIS_PREFIX + requestId);
            if (task != null) {
                taskCache.put(requestId, task);
            }
        }
        return task;
    }

    /** 触发回调通知 */
    private void triggerCallback(AsyncTask task) {
        try {
            log.info(
                    "触发回调: requestId={}, url={}, status={}",
                    task.getRequestId(),
                    task.getCallbackUrl(),
                    task.getStatus());
            // TODO: 使用RestTemplate/WebClient异步调用callbackUrl
            // 回调内容：{requestId, status, result, error, completedAt}
        } catch (Exception e) {
            log.error("回调通知失败: requestId={}", task.getRequestId(), e);
        }
    }

    private void saveToRedis(AsyncTask task) {
        try {
            redisTemplate
                    .opsForValue()
                    .set(REDIS_PREFIX + task.getRequestId(), task, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("异步任务Redis存储失败: requestId={}", task.getRequestId(), e);
        }
    }
}
