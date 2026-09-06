package com.xwms.common.crypto;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 敏感字段JSON序列化脱敏器
 *
 * <p>在对象序列化为JSON时，自动对@Sensitive注解的字段进行脱敏。 这样前端收到的数据已经是脱敏后的，无需前端处理。
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;Sensitive(type = SensitiveType.PHONE)
 * &#64;JsonSerialize(using = SensitiveSerializer.class)
 * private String phone;
 * </pre>
 *
 * 或者通过@Sensitive注解自动识别（需要配合SensitiveAnnotationIntrospector）
 */
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private Sensitive.SensitiveType type;
    private int prefixKeep;
    private int suffixKeep;
    private char mask;

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (type == Sensitive.SensitiveType.CUSTOM && prefixKeep == 0 && suffixKeep == 0) {
            // 未配置具体类型，默认全部脱敏
            gen.writeString("*".repeat(value.length()));
            return;
        }
        if (type == Sensitive.SensitiveType.CUSTOM) {
            gen.writeString(SensitiveMaskUtil.maskCustom(value, prefixKeep, suffixKeep, mask));
        } else {
            gen.writeString(SensitiveMaskUtil.mask(value, type));
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException {
        if (property == null) {
            return this;
        }
        Sensitive sensitive = property.getAnnotation(Sensitive.class);
        if (sensitive != null) {
            return new SensitiveSerializer(
                    sensitive.type(),
                    sensitive.prefixKeep(),
                    sensitive.suffixKeep(),
                    sensitive.mask());
        }
        return this;
    }
}
