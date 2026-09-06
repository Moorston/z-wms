package com.xwms.common.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM 加密工具
 *
 * <p>加密算法：AES/GCM/NoPadding（认证加密，防篡改） 密钥长度：256位 输出格式：Base64(IV[12字节] + 密文 + Tag[16字节])
 *
 * <p>特性： 1. 每次加密随机IV，相同明文加密结果不同（防字典攻击） 2. GCM模式提供认证，防止密文被篡改 3. 支持空值和空字符串（不加密，直接返回）
 *
 * <p>密钥配置： - application.yaml: wms.crypto.secret-key=xxx（32字节Base64） - 环境变量:
 * WMS_CRYPTO_SECRET_KEY（优先级更高） - 生产环境必须通过环境变量或KMS注入，禁止硬编码
 */
@Slf4j
@Component
public class AesEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12; // GCM推荐12字节
    private static final int TAG_LENGTH = 128; // 128位认证标签
    private static final String KEY_ALGORITHM = "AES";

    @Value("${wms.crypto.secret-key:}")
    private String secretKey;

    @Value("${wms.crypto.enabled:true}")
    private boolean enabled;

    private SecretKeySpec keySpec;
    private final SecureRandom random = new SecureRandom();

    /**
     * 加密
     *
     * @param plainText 明文
     * @return Base64编码的密文（IV+密文+Tag）
     */
    public String encrypt(String plainText) {
        if (!enabled || plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] key = getKey();
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM), spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 组合 IV + 密文
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("加密失败: {}", e.getMessage(), e);
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密
     *
     * @param cipherText Base64编码的密文
     * @return 明文
     */
    public String decrypt(String cipherText) {
        if (!enabled || cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            byte[] key = getKey();
            byte[] combined = Base64.getDecoder().decode(cipherText);

            // 分离 IV 和密文
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, KEY_ALGORITHM), spec);

            byte[] plainText = cipher.doFinal(encrypted);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("解密失败: {}", e.getMessage(), e);
            throw new RuntimeException("解密失败", e);
        }
    }

    /** 判断是否为加密后的密文（简单判断：Base64可解码且长度>IV长度） */
    public boolean isEncrypted(String text) {
        if (text == null || text.length() < IV_LENGTH * 2) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(text);
            return decoded.length > IV_LENGTH;
        } catch (Exception e) {
            return false;
        }
    }

    /** 获取加密密钥 优先级：环境变量 > 配置文件 > 默认密钥（仅开发用） */
    private byte[] getKey() {
        String key = System.getenv("WMS_CRYPTO_SECRET_KEY");
        if (key == null || key.isEmpty()) {
            key = secretKey;
        }
        if (key == null || key.isEmpty()) {
            // 开发环境默认密钥（32字节），生产环境必须替换
            key = "x-wms-default-aes-256-key-2026!!";
            log.warn("使用默认加密密钥，生产环境请配置 WMS_CRYPTO_SECRET_KEY 环境变量");
        }
        // 确保32字节（256位）
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            return padded;
        }
        return keyBytes;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
