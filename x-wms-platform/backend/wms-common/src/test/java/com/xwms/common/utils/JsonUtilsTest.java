package com.xwms.common.utils;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JSON工具类单元测试
 */
class JsonUtilsTest {

    @Test
    void testToJson_andParseObject() {
        Map<String, Object> data = Map.of("name", "test", "age", 25);
        String json = JsonUtils.toJson(data);
        assertNotNull(json);
        assertTrue(json.contains("test"));

        Map parsed = JsonUtils.parseObject(json, Map.class);
        assertEquals("test", parsed.get("name"));
    }

    @Test
    void testParseArray() {
        String json = "[{\"name\":\"a\"},{\"name\":\"b\"}]";
        List<Map> list = JsonUtils.parseArray(json, Map.class);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).get("name"));
    }

    @Test
    void testConvert() {
        Map<String, Object> source = Map.of("sku", "SKU001", "qty", 10);
        TestBean bean = JsonUtils.convert(source, TestBean.class);
        assertEquals("SKU001", bean.getSku());
        assertEquals(10, bean.getQty());
    }

    @Test
    void testToMap() {
        TestBean bean = new TestBean();
        bean.setSku("SKU001");
        bean.setQty(10);
        Map<String, Object> map = JsonUtils.toMap(bean);
        assertEquals("SKU001", map.get("sku"));
        assertEquals(10, map.get("qty"));
    }

    @Test
    void testNullJson() {
        assertNull(JsonUtils.parseObject(null, Map.class));
        assertNull(JsonUtils.parseArray(null, Map.class));
    }

    // 测试用Bean
    public static class TestBean {
        private String sku;
        private Integer qty;
        public String getSku() { return sku; }
        public void setSku(String sku) { this.sku = sku; }
        public Integer getQty() { return qty; }
        public void setQty(Integer qty) { this.qty = qty; }
    }
}
