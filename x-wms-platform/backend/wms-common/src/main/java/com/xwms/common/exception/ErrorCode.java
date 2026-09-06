package com.xwms.common.exception;

import lombok.Getter;

/** 错误码枚举 */
@Getter
public enum ErrorCode {
    SUCCESS(200, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    RATE_LIMITED(429, "请求过于频繁"),
    TIMEOUT(408, "请求超时"),

    // 库存相关 1000-1999
    INVENTORY_NOT_ENOUGH(1001, "库存不足"),
    INVENTORY_ALLOCATE_FAILED(1002, "库存预占失败"),
    INVENTORY_DEDUCT_FAILED(1003, "库存扣减失败"),

    // 订单相关 2000-2999
    ORDER_NOT_FOUND(2001, "订单不存在"),
    ORDER_STATUS_ERROR(2002, "订单状态错误"),
    ORDER_ALREADY_EXISTS(2003, "订单已存在"),

    // 入库相关 3000-3999
    ASN_NOT_FOUND(3001, "ASN不存在"),
    RECEIVE_QTY_ERROR(3002, "收货数量错误"),

    // 出库相关 4000-4999
    WAVE_NOT_FOUND(4001, "波次不存在"),
    PICK_TASK_ERROR(4002, "拣货任务错误"),

    // 系统相关 9000-9999
    PLUGIN_LOAD_FAILED(9001, "插件加载失败"),
    RULE_EXECUTE_FAILED(9002, "规则执行失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
