package com.xwms.common.utils;

/** 雪花算法ID生成器 64位：1位符号 + 41位时间戳 + 10位机器ID + 12位序列号 支持分布式唯一ID，趋势递增 */
public class IdGenerator {

    /** 起始时间戳（2024-01-01） */
    private static final long START_TIMESTAMP = 1704067200000L;

    /** 机器ID位数 */
    private static final long WORKER_ID_BITS = 10L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大机器ID */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 最大序列号 */
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    /** 机器ID左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    private final long workerId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public IdGenerator(long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId must be between 0 and " + MAX_WORKER_ID);
        }
        this.workerId = workerId;
    }

    /** 生成唯一ID */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards, refusing to generate id");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    /** 字符串ID */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }
}
