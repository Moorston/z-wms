package com.xwms.common.utils;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** JSON工具类（基于Jackson） */
public class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static <T> T parseObject(String json, TypeReference<T> typeRef) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(
                    json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static Map<String, Object> parseObject(String json) {
        if (json == null) return null;
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static Map<String, Object> toMap(Object obj) {
        if (obj == null) return null;
        return MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    public static <T> T convert(Object source, Class<T> clazz) {
        if (source == null) return null;
        return MAPPER.convertValue(source, clazz);
    }
}
