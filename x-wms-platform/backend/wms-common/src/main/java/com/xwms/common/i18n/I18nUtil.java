package com.xwms.common.i18n;

import java.util.Locale;

import jakarta.annotation.Resource;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/** 国际化消息工具类 提供静态方法获取国际化消息 */
@Component
public class I18nUtil {

    private static MessageSource messageSource;

    @Resource
    public void setMessageSource(MessageSource messageSource) {
        I18nUtil.messageSource = messageSource;
    }

    /**
     * 获取国际化消息（使用当前Locale）
     *
     * @param code 消息码
     * @return 国际化消息
     */
    public static String getMessage(String code) {
        return messageSource.getMessage(code, null, code, getCurrentLocale());
    }

    /**
     * 获取国际化消息（带参数）
     *
     * @param code 消息码
     * @param args 参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, code, getCurrentLocale());
    }

    /**
     * 获取国际化消息（指定Locale）
     *
     * @param code 消息码
     * @param locale 语言
     * @param args 参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }

    /** 获取当前Locale */
    public static Locale getCurrentLocale() {
        Locale locale = LocaleContextHolder.getLocale();
        return locale != null ? locale : Locale.SIMPLIFIED_CHINESE;
    }

    /** 设置当前Locale */
    public static void setCurrentLocale(Locale locale) {
        LocaleContextHolder.setLocale(locale);
    }
}
