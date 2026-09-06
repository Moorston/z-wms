package com.xwms.common.plugin.extension;

import java.util.Map;

import lombok.Data;

/** 扩展点执行结果 行业插件返回此对象表示拦截核心流程 */
@Data
public class ExtensionResult {
    /** 是否拦截（true=拦截，核心流程按此结果执行） */
    private boolean intercepted;

    /** 拦截原因/提示信息 */
    private String message;

    /** 扩展数据（如指定批号、指定库位、质检方案） */
    private Map<String, Object> data;

    /** 是否拒绝执行（true=抛出异常，终止流程） */
    private boolean rejected;

    public static ExtensionResult pass() {
        ExtensionResult r = new ExtensionResult();
        r.setIntercepted(false);
        return r;
    }

    public static ExtensionResult intercept(String message, Map<String, Object> data) {
        ExtensionResult r = new ExtensionResult();
        r.setIntercepted(true);
        r.setMessage(message);
        r.setData(data);
        return r;
    }

    public static ExtensionResult reject(String message) {
        ExtensionResult r = new ExtensionResult();
        r.setIntercepted(true);
        r.setRejected(true);
        r.setMessage(message);
        return r;
    }
}
