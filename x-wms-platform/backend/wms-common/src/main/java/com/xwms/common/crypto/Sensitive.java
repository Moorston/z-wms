package com.xwms.common.crypto;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 敏感字段注解
 *
 * <p>标记需要加密存储和脱敏展示的字段。 配合EncryptTypeHandler实现数据库加密存储， 配合SensitiveSerializer实现JSON序列化时脱敏。
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;Sensitive(type = SensitiveType.PHONE)
 * &#64;TableField(typeHandler = EncryptTypeHandler.class)
 * private String phone;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    /** 敏感数据类型（决定脱敏方式） */
    SensitiveType type() default SensitiveType.CUSTOM;

    /** 自定义脱敏前缀保留位数 */
    int prefixKeep() default 0;

    /** 自定义脱敏后缀保留位数 */
    int suffixKeep() default 0;

    /** 脱敏掩码字符 */
    char mask() default '*';

    /** 敏感数据类型枚举 */
    enum SensitiveType {
        /** 手机号：138****1234 */
        PHONE,
        /** 身份证：440***********1234 */
        ID_CARD,
        /** 邮箱：a**@example.com */
        EMAIL,
        /** 姓名：张* */
        NAME,
        /** 地址：广东省佛山市**** */
        ADDRESS,
        /** 银行卡号：6222 **** **** 1234 */
        BANK_CARD,
        /** 自定义 */
        CUSTOM
    }
}
