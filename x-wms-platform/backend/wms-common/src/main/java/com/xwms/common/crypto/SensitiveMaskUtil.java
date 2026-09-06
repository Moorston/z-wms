package com.xwms.common.crypto;

/**
 * 敏感数据脱敏工具
 *
 * <p>支持多种脱敏策略： - 手机号：138****1234 - 身份证：440***********1234 - 邮箱：a**@example.com - 姓名：张* -
 * 地址：广东省佛山市**** - 银行卡：6222 **** **** 1234 - 自定义：前后保留N位
 */
public class SensitiveMaskUtil {

    private SensitiveMaskUtil() {}

    private static final String MASK = "*";

    /** 手机号脱敏：138****1234 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 身份证脱敏：440***********1234 */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) {
            return idCard;
        }
        return idCard.substring(0, 3) + "***********" + idCard.substring(idCard.length() - 4);
    }

    /** 邮箱脱敏：a**@example.com */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String username = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (username.length() <= 2) {
            return username.charAt(0) + "**" + domain;
        }
        return username.substring(0, 1) + "**" + domain;
    }

    /** 姓名脱敏：张* / 张*三 */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        if (name.length() == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    /** 地址脱敏：广东省佛山市**** 保留前6个字符（省市级），后面脱敏 */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "****";
    }

    /** 银行卡脱敏：6222 **** **** 1234 */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        String prefix = bankCard.substring(0, 4);
        String suffix = bankCard.substring(bankCard.length() - 4);
        return prefix + " **** **** " + suffix;
    }

    /** 自定义脱敏：保留前prefix位和后suffix位 */
    public static String maskCustom(String value, int prefixKeep, int suffixKeep, char mask) {
        if (value == null || value.length() <= prefixKeep + suffixKeep) {
            return value;
        }
        String prefix = value.substring(0, prefixKeep);
        String suffix = value.substring(value.length() - suffixKeep);
        int maskCount = value.length() - prefixKeep - suffixKeep;
        return prefix + String.valueOf(mask).repeat(maskCount) + suffix;
    }

    /** 根据敏感类型自动脱敏 */
    public static String mask(String value, Sensitive.SensitiveType type) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return switch (type) {
            case PHONE -> maskPhone(value);
            case ID_CARD -> maskIdCard(value);
            case EMAIL -> maskEmail(value);
            case NAME -> maskName(value);
            case ADDRESS -> maskAddress(value);
            case BANK_CARD -> maskBankCard(value);
            case CUSTOM -> maskCustom(value, 1, 1, '*');
        };
    }
}
