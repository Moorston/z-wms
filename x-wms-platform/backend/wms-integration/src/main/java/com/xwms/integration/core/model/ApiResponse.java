package com.xwms.integration.core.model;

import java.time.LocalDateTime;

import lombok.Data;

/** API响应封装 */
@Data
public class ApiResponse {
    /** 响应码：200成功，4xx客户端错误，5xx服务端错误 */
    private int code;

    /** 响应消息 */
    private String message;

    /** 响应数据 */
    private Object data;

    /** 请求ID（TraceId） */
    private String requestId;

    /** 响应时间 */
    private LocalDateTime responseTime = LocalDateTime.now();

    /** 处理耗时（毫秒） */
    private long durationMs;

    /** 适配器原始响应（调试用） */
    private Object rawResponse;

    public static ApiResponse success(Object data) {
        ApiResponse r = new ApiResponse();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    public static ApiResponse error(int code, String message) {
        ApiResponse r = new ApiResponse();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    public static ApiResponse error(String message) {
        return error(500, message);
    }

    /** 是否成功（2xx 视为成功） */
    public boolean isSuccess() {
        return code >= 200 && code < 300;
    }
}
