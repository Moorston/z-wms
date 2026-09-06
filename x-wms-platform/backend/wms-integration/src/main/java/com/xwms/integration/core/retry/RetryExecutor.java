package com.xwms.integration.core.retry;

import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.xwms.integration.core.model.ApiDefinition;

import lombok.extern.slf4j.Slf4j;

/** 重试执行器 指数退避重试策略：1s -> 2s -> 4s -> 8s（上限30s） 只对可重试异常重试（网络超时/5xx），业务异常不重试 */
@Slf4j
@Service
public class RetryExecutor {

    /**
     * 带重试执行
     *
     * @param supplier 业务逻辑
     * @param config 重试配置
     * @return 执行结果
     */
    public <T> T executeWithRetry(Supplier<T> supplier, ApiDefinition.RetryConfig config) {
        int maxRetries = config != null ? config.getMaxRetries() : 3;
        long backoff = config != null ? config.getInitialBackoffMs() : 1000;
        double multiplier = config != null ? config.getMultiplier() : 2.0;
        long maxBackoff = config != null ? config.getMaxBackoffMs() : 30000;

        Exception lastException = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                if (!isRetryable(e) || attempt == maxRetries) {
                    log.error(
                            "重试最终失败: attempt={}/{}, error={}", attempt, maxRetries, e.getMessage());
                    break;
                }
                long waitTime =
                        Math.min((long) (backoff * Math.pow(multiplier, attempt)), maxBackoff);
                log.warn(
                        "重试中: attempt={}/{}, wait={}ms, error={}",
                        attempt + 1,
                        maxRetries,
                        waitTime,
                        e.getMessage());
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException("重试" + maxRetries + "次后仍然失败", lastException);
    }

    /** 判断是否可重试 可重试：网络超时、连接拒绝、5xx服务端错误 不可重试：4xx客户端错误、业务校验失败 */
    private boolean isRetryable(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        // 不可重试的异常
        if (msg.contains("400")
                || msg.contains("401")
                || msg.contains("403")
                || msg.contains("404")
                || msg.contains("validation")
                || msg.contains("invalid")
                || msg.contains("duplicate")) {
            return false;
        }
        // 可重试的异常
        return msg.contains("timeout")
                || msg.contains("connection")
                || msg.contains("500")
                || msg.contains("502")
                || msg.contains("503")
                || msg.contains("504")
                || e instanceof java.net.SocketTimeoutException
                || e instanceof java.net.ConnectException;
    }
}
