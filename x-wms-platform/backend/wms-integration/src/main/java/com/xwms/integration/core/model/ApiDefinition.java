package com.xwms.integration.core.model;

import java.util.Map;

import lombok.Data;

/** API定义 每个外部系统对接的API都注册为一个ApiDefinition 包含：路径、方法、目标适配器、鉴权方式、限流配置、转换规则 */
@Data
public class ApiDefinition {
    /** API唯一标识 */
    private String apiId;

    /** API名称 */
    private String apiName;

    /** 请求路径（如 /api/v1/erp/order） */
    private String path;

    /** HTTP方法：GET/POST/PUT/DELETE */
    private String method;

    /** 目标适配器ID（如 sap-erp / sf-express / taobao） */
    private String adapterId;

    /** 目标适配器方法 */
    private String adapterMethod;

    /** 鉴权方式：NONE/API_KEY/HMAC/OAUTH2 */
    private String authType;

    /** 限流配置 */
    private RateLimitConfig rateLimit;

    /** 数据转换规则（请求映射/响应映射） */
    private TransformConfig transform;

    /** 是否异步（异步走Kafka） */
    private boolean async;

    /** 超时时间（毫秒） */
    private int timeoutMs = 5000;

    /** 重试配置 */
    private RetryConfig retry;

    /** 状态：ENABLED/DISABLED */
    private String status;

    @Data
    public static class RateLimitConfig {
        /** 限流算法：TOKEN_BUCKET/SLIDING_WINDOW */
        private String algorithm = "TOKEN_BUCKET";

        /** 每秒请求数 */
        private int qps = 100;

        /** 突发容量 */
        private int burst = 200;
    }

    @Data
    public static class TransformConfig {
        /** 请求转换规则（JSONPath映射） */
        private Map<String, String> requestMapping;

        /** 响应转换规则 */
        private Map<String, String> responseMapping;

        /** 是否使用默认转换 */
        private boolean useDefault = true;
    }

    @Data
    public static class RetryConfig {
        /** 最大重试次数 */
        private int maxRetries = 3;

        /** 初始退避时间（毫秒） */
        private long initialBackoffMs = 1000;

        /** 退避乘数 */
        private double multiplier = 2.0;

        /** 最大退避时间 */
        private long maxBackoffMs = 30000;
    }
}
