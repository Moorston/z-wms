package com.xwms.common.crypto;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 敏感字段加密TypeHandler
 *
 * <p>MyBatis读写时自动加解密： - 写入数据库：明文 → 加密 → 密文存储 - 读取数据库：密文 → 解密 → 明文使用
 *
 * <p>使用方式：
 *
 * <pre>
 * &#64;TableField(typeHandler = EncryptTypeHandler.class)
 * private String phone;
 * </pre>
 *
 * 注意： 1. 使用该TypeHandler的字段，查询结果必须设置 autoResultMap = true 2. 加密字段无法直接在SQL中做模糊查询，需使用加密检索方案 3.
 * 加密字段不能作为排序字段
 */
@Slf4j
@Component
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class EncryptTypeHandler extends BaseTypeHandler<String> {

    private static AesEncryptor encryptor;

    /** Spring注入静态字段（TypeHandler由MyBatis实例化，不经过Spring） */
    @org.springframework.beans.factory.annotation.Autowired
    public void setEncryptor(AesEncryptor encryptor) {
        EncryptTypeHandler.encryptor = encryptor;
    }

    @Override
    public void setNonNullParameter(
            PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (encryptor == null) {
            ps.setString(i, parameter);
            return;
        }
        // 写入时加密
        String encrypted = encryptor.encrypt(parameter);
        ps.setString(i, encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return decrypt(value);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return decrypt(value);
    }

    private String decrypt(String value) {
        if (value == null || value.isEmpty() || encryptor == null) {
            return value;
        }
        // 如果不是密文（历史数据未加密），直接返回
        if (!encryptor.isEncrypted(value)) {
            return value;
        }
        try {
            return encryptor.decrypt(value);
        } catch (Exception e) {
            // 解密失败，可能是历史明文数据，直接返回
            log.warn("解密失败，返回原始值: {}", e.getMessage());
            return value;
        }
    }
}
