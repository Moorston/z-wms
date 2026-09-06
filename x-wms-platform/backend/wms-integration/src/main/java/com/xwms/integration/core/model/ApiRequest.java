package com.xwms.integration.core.model;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

/** API请求封装 */
@Data
public class ApiRequest {
    /** 请求唯一ID（TraceId） */
    private String requestId;

    /** 调用方应用ID */
    private String appId;

    /** API路径 */
    private String path;

    /** HTTP方法 */
    private String method;

    /** 请求头 */
    private Map<String, String> headers;

    /** 请求参数 */
    private Map<String, Object> params;

    /** 请求体 */
    private Object body;

    /** 客户端IP */
    private String clientIp;

    /** 请求时间 */
    private LocalDateTime requestTime = LocalDateTime.now();

    /** 鉴权信息 */
    private AuthInfo authInfo;

    @Data
    public static class AuthInfo {
        /** API Key */
        private String apiKey;

        /** HMAC签名 */
        private String signature;

        /** 时间戳 */
        private String timestamp;

        /** 随机数 */
        private String nonce;

        /** Token */
        private String token;
    }
}
