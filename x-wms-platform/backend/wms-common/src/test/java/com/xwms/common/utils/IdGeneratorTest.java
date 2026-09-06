package com.xwms.common.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 雪花ID生成器单元测试
 */
class IdGeneratorTest {

    @Test
    void testNextId_unique() {
        IdGenerator generator = new IdGenerator(1);
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        assertNotEquals(id1, id2, "连续生成的ID应该唯一");
    }

    @Test
    void testNextId_trendIncreasing() {
        IdGenerator generator = new IdGenerator(1);
        long id1 = generator.nextId();
        long id2 = generator.nextId();
        long id3 = generator.nextId();
        assertTrue(id2 > id1 && id3 > id2, "ID应该趋势递增");
    }

    @Test
    void testNextIdStr() {
        IdGenerator generator = new IdGenerator(1);
        String idStr = generator.nextIdStr();
        assertNotNull(idStr);
        assertFalse(idStr.isEmpty());
        assertTrue(idStr.matches("\\d+"), "ID字符串应该是数字");
    }

    @Test
    void testDifferentWorkerId() {
        IdGenerator gen1 = new IdGenerator(1);
        IdGenerator gen2 = new IdGenerator(2);
        long id1 = gen1.nextId();
        long id2 = gen2.nextId();
        // 不同机器ID生成的ID不同（机器位不同）
        assertNotEquals(id1 >> 12 & 0x3FF, id2 >> 12 & 0x3FF,
                "不同workerId的机器位应该不同");
    }

    @Test
    void testInvalidWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(-1));
        assertThrows(IllegalArgumentException.class, () -> new IdGenerator(1024));
    }

    @Test
    void testBatchUnique() {
        IdGenerator generator = new IdGenerator(1);
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (int i = 0; i < 10000; i++) {
            ids.add(generator.nextId());
        }
        assertEquals(10000, ids.size(), "批量生成10000个ID应该全部唯一");
    }
}
