package com.xwms.common.tenant.context;

import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;

/**
 * 货主上下文（租户级隔离） 通过ThreadLocal持有当前请求的货主编码，MyBatis-Plus租户插件自动读取并拼接SQL。
 *
 * <p>使用场景： 1. HTTP请求：OwnerFilter从JWT解析ownerCode并设置 2.
 * Feign调用：FeignRequestInterceptor透传X-Owner-Code请求头 3.
 * Kafka消费：KafkaTraceConsumerInterceptor从消息头读取并设置 4. 异步任务：需手动调用OwnerContext.runWith()包裹
 *
 * <p>注意：必须在请求结束时调用clear()，否则线程池复用会导致数据串货主
 */
@Slf4j
public class OwnerContext {

    private static final ThreadLocal<String> OWNER_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE_HOLDER = new ThreadLocal<>();

    private OwnerContext() {}

    /** 设置当前货主编码 */
    public static void set(String ownerCode) {
        OWNER_HOLDER.set(ownerCode);
    }

    /**
     * 获取当前货主编码
     *
     * @return 货主编码，未设置返回null
     */
    public static String get() {
        return OWNER_HOLDER.get();
    }

    /** 获取当前货主编码，为空时返回默认值 */
    public static String getOrDefault(String defaultOwner) {
        String owner = OWNER_HOLDER.get();
        return owner != null ? owner : defaultOwner;
    }

    /** 是否设置了货主 */
    public static boolean hasOwner() {
        return OWNER_HOLDER.get() != null;
    }

    /** 清除当前货主（必须在请求结束时调用） */
    public static void clear() {
        OWNER_HOLDER.remove();
        IGNORE_HOLDER.remove();
    }

    /** 忽略租户隔离（用于系统级查询、跨货主统计） 使用try-finally确保恢复 */
    public static void setIgnore(boolean ignore) {
        IGNORE_HOLDER.set(ignore);
    }

    /** 是否忽略租户隔离 */
    public static boolean isIgnore() {
        return Boolean.TRUE.equals(IGNORE_HOLDER.get());
    }

    /** 在指定货主上下文中执行（自动设置和清除） 用于异步任务、定时任务等非HTTP场景 */
    public static <T> T runWith(String ownerCode, Supplier<T> supplier) {
        String previous = OWNER_HOLDER.get();
        try {
            OWNER_HOLDER.set(ownerCode);
            return supplier.get();
        } finally {
            if (previous != null) {
                OWNER_HOLDER.set(previous);
            } else {
                OWNER_HOLDER.remove();
            }
        }
    }

    /** 在指定货主上下文中执行（无返回值） */
    public static void runWith(String ownerCode, Runnable runnable) {
        String previous = OWNER_HOLDER.get();
        try {
            OWNER_HOLDER.set(ownerCode);
            runnable.run();
        } finally {
            if (previous != null) {
                OWNER_HOLDER.set(previous);
            } else {
                OWNER_HOLDER.remove();
            }
        }
    }

    /** 在忽略租户隔离的上下文中执行 用于系统级查询、管理员跨货主操作 */
    public static <T> T runIgnore(Supplier<T> supplier) {
        Boolean previous = IGNORE_HOLDER.get();
        try {
            IGNORE_HOLDER.set(true);
            return supplier.get();
        } finally {
            if (previous != null) {
                IGNORE_HOLDER.set(previous);
            } else {
                IGNORE_HOLDER.remove();
            }
        }
    }
}
