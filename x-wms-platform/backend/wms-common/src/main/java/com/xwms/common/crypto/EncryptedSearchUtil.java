package com.xwms.common.crypto;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 加密字段检索工具
 *
 * <p>加密字段的检索方案： 1. 精确查询：由于AES-GCM每次加密IV不同，相同明文加密结果不同，无法直接等值匹配。
 * 解决方案：使用确定性加密（固定IV）生成检索哈希，存储在单独的_hash字段中。 2. 模糊查询：无法在数据库层做LIKE，需查出数据后在应用层解密过滤。
 *
 * <p>使用方式：
 *
 * <pre>
 * // 精确查询：先计算检索哈希，再用哈希字段匹配
 * String phoneHash = encryptedSearchUtil.deterministicEncrypt("13800138000");
 * Customer customer = customerMapper.selectOne(
 *     new LambdaQueryWrapper<Customer>().eq(Customer::getPhoneHash, phoneHash));
 *
 * // 模糊查询：查出所有数据，应用层解密后过滤
 * List<Customer> all = customerMapper.selectList(null);
 * List<Customer> result = encryptedSearchUtil.filterByDecryptedField(
 *     all, Customer::getPhone, keyword -> keyword.contains("138"));
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EncryptedSearchUtil {

    private final AesEncryptor aesEncryptor;

    /** 确定性加密（用于检索哈希） 使用固定IV，相同明文加密结果相同，可用于等值查询。 注意：仅用于检索，不用于存储（存储使用随机IV更安全） */
    public String deterministicEncrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        // 使用SHA-256哈希作为检索索引（单向，不可逆）
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash =
                    digest.digest(plainText.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("确定性加密失败: {}", e.getMessage());
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 应用层模糊查询（解密后过滤）
     *
     * @param list 待过滤列表
     * @param fieldExtractor 字段提取函数
     * @param predicate 过滤条件（参数为解密后的明文）
     * @return 过滤后的列表
     */
    public <T> List<T> filterByDecryptedField(
            List<T> list,
            java.util.function.Function<T, String> fieldExtractor,
            java.util.function.Predicate<String> predicate) {
        return list.stream()
                .filter(
                        item -> {
                            String encrypted = fieldExtractor.apply(item);
                            if (encrypted == null) return false;
                            try {
                                String decrypted = aesEncryptor.decrypt(encrypted);
                                return predicate.test(decrypted);
                            } catch (Exception e) {
                                // 解密失败，可能是历史明文数据
                                return predicate.test(encrypted);
                            }
                        })
                .collect(Collectors.toList());
    }

    /** 批量解密字段（用于列表展示） */
    public <T> List<T> decryptField(
            List<T> list,
            java.util.function.BiConsumer<T, String> fieldSetter,
            java.util.function.Function<T, String> fieldGetter) {
        list.forEach(
                item -> {
                    String encrypted = fieldGetter.apply(item);
                    if (encrypted != null && aesEncryptor.isEncrypted(encrypted)) {
                        try {
                            fieldSetter.accept(item, aesEncryptor.decrypt(encrypted));
                        } catch (Exception e) {
                            log.warn("批量解密失败: {}", e.getMessage());
                        }
                    }
                });
        return list;
    }
}
