package com.xwms.integration.api.transform;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

/**
 * 数据转换引擎 解决外部系统数据格式与WMS内部格式不一致的问题 支持： 1. 字段映射（JSONPath表达式） 2. 类型转换 3. 默认值填充 4. 嵌套结构转换 5.
 * 自定义转换器（SPI扩展）
 */
@Slf4j
@Service
public class DataTransformEngine {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 请求转换：外部格式 -> WMS内部格式
     *
     * @param source 原始请求数据
     * @param mapping 字段映射规则（目标字段 -> 源JSONPath）
     * @return 转换后的数据
     */
    public ObjectNode transformRequest(Object source, Map<String, String> mapping) {
        if (mapping == null || mapping.isEmpty()) {
            return MAPPER.valueToTree(source);
        }
        ObjectNode src = MAPPER.valueToTree(source);
        ObjectNode target = MAPPER.createObjectNode();
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String targetField = entry.getKey();
            String sourcePath = entry.getValue();
            JsonNode value = extractByPath(src, sourcePath);
            if (value != null && !value.isNull()) {
                setByPath(target, targetField, value);
            }
        }
        log.debug("请求转换完成: 字段数={}", target.size());
        return target;
    }

    /** 响应转换：WMS内部格式 -> 外部格式 */
    public ObjectNode transformResponse(Object source, Map<String, String> mapping) {
        return transformRequest(source, mapping); // 转换逻辑相同，方向相反
    }

    /** 按JSONPath提取值（简化实现，支持a.b.c） */
    private JsonNode extractByPath(JsonNode node, String path) {
        if (path == null || path.isBlank()) return null;
        String[] parts = path.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current == null || !current.isObject()) return null;
            current = current.get(part);
            if (current == null) return null;
        }
        return current;
    }

    /** 按路径设置值（支持a.b.c嵌套） */
    private void setByPath(ObjectNode node, String path, JsonNode value) {
        String[] parts = path.split("\\.");
        ObjectNode current = node;
        for (int i = 0; i < parts.length - 1; i++) {
            JsonNode child = current.get(parts[i]);
            if (child == null || !child.isObject()) {
                child = current.putObject(parts[i]);
            }
            current = (ObjectNode) child;
        }
        current.set(parts[parts.length - 1], value);
    }

    /** 类型转换 */
    public Object convertType(Object value, String targetType) {
        if (value == null) return null;
        return switch (targetType) {
            case "STRING" -> value.toString();
            case "INTEGER" -> Integer.parseInt(value.toString());
            case "LONG" -> Long.parseLong(value.toString());
            case "DOUBLE" -> Double.parseDouble(value.toString());
            case "BOOLEAN" -> Boolean.parseBoolean(value.toString());
            case "DATE" -> value.toString(); // TODO: 日期格式化
            default -> value;
        };
    }
}
