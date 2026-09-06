package com.xwms.common.exception;

import com.xwms.common.i18n.I18nUtil;

import lombok.Getter;

/** 业务异常 支持国际化消息： 1. 直接传入消息文本 2. 传入消息码，自动从i18n消息源获取 3. 传入消息码+参数，自动格式化 */
@Getter
public class BizException extends RuntimeException {
    private final Integer code;

    /** 消息码（用于国际化） */
    private final String messageCode;

    /** 消息参数 */
    private final Object[] messageArgs;

    public BizException(String message) {
        super(message);
        this.code = 500;
        this.messageCode = null;
        this.messageArgs = null;
    }

    public BizException(Integer code, String message) {
        super(message);
        this.code = code;
        this.messageCode = null;
        this.messageArgs = null;
    }

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.messageCode = null;
        this.messageArgs = null;
    }

    /**
     * 使用国际化消息码创建异常
     *
     * @param code 错误码
     * @param messageCode 消息码（如 "inventory.not_enough"）
     * @param args 消息参数
     */
    public BizException(Integer code, String messageCode, Object... args) {
        super(I18nUtil.getMessage(messageCode, args));
        this.code = code;
        this.messageCode = messageCode;
        this.messageArgs = args;
    }

    /** 使用国际化消息码创建异常（默认错误码500） */
    public static BizException of(String messageCode, Object... args) {
        return new BizException(500, messageCode, args);
    }

    /** 使用国际化消息码+错误码创建异常 */
    public static BizException of(Integer code, String messageCode, Object... args) {
        return new BizException(code, messageCode, args);
    }

    /** 获取国际化消息（根据当前Locale） */
    public String getI18nMessage() {
        if (messageCode != null) {
            return I18nUtil.getMessage(messageCode, messageArgs);
        }
        return getMessage();
    }
}
