package com.xwms.integration.api.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.xwms.integration.core.model.ApiRequest;

import lombok.extern.slf4j.Slf4j;

/**
 * API鉴权服务 支持4种鉴权方式： 1. NONE - 无鉴权（内部调用） 2. API_KEY - 简单API Key 3. HMAC - HMAC-SHA256签名（防篡改+防重放） 4.
 * OAUTH2 - OAuth2 Bearer Token
 */
@Slf4j
@Service
public class ApiAuthService {

    /** appId -> appSecret */
    private final Map<String, String> appSecrets = new ConcurrentHashMap<>();

    /** appId -> 权限范围 */
    private final Map<String, String> appScopes = new ConcurrentHashMap<>();

    /** nonce缓存（防重放，5分钟有效） */
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();

    private static final long NONCE_TTL_MS = 5 * 60 * 1000;
    private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000;

    /** 注册应用凭证 */
    public void registerApp(String appId, String appSecret, String scope) {
        appSecrets.put(appId, appSecret);
        appScopes.put(appId, scope);
        log.info("注册应用: appId={}, scope={}", appId, scope);
    }

    /**
     * 鉴权入口
     *
     * @return true=鉴权通过，false=拒绝
     */
    public boolean authenticate(ApiRequest request, String authType) {
        return switch (authType) {
            case "NONE" -> true;
            case "API_KEY" -> validateApiKey(request);
            case "HMAC" -> validateHmac(request);
            case "OAUTH2" -> validateOAuth2(request);
            default -> false;
        };
    }

    /** API Key鉴权 */
    private boolean validateApiKey(ApiRequest request) {
        String apiKey = request.getHeaders().get("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API Key鉴权失败: 缺少X-API-Key头");
            return false;
        }
        return appSecrets.containsValue(apiKey);
    }

    /**
     * HMAC-SHA256签名鉴权 签名 = Base64(HMAC-SHA256(appSecret, method + path + timestamp + nonce + body))
     * 防重放：timestamp在5分钟内 + nonce唯一
     */
    private boolean validateHmac(ApiRequest request) {
        var auth = request.getAuthInfo();
        if (auth == null || auth.getApiKey() == null || auth.getSignature() == null) {
            return false;
        }
        String appId = auth.getApiKey();
        String appSecret = appSecrets.get(appId);
        if (appSecret == null) return false;

        // 防重放：时间戳检查
        long timestamp = Long.parseLong(auth.getTimestamp());
        if (Math.abs(System.currentTimeMillis() - timestamp) > TIMESTAMP_TOLERANCE_MS) {
            log.warn("HMAC鉴权失败: 时间戳超出容忍范围, appId={}", appId);
            return false;
        }
        // 防重放：nonce唯一
        String nonceKey = appId + ":" + auth.getNonce();
        if (nonceCache.containsKey(nonceKey)) {
            log.warn("HMAC鉴权失败: nonce重复, appId={}", appId);
            return false;
        }
        nonceCache.put(nonceKey, System.currentTimeMillis());

        // 签名验证
        String signContent =
                request.getMethod()
                        + request.getPath()
                        + auth.getTimestamp()
                        + auth.getNonce()
                        + request.getBody();
        String expectedSignature = hmacSha256(appSecret, signContent);
        return expectedSignature.equals(auth.getSignature());
    }

    /** OAuth2 Token鉴权 */
    private boolean validateOAuth2(ApiRequest request) {
        String authHeader = request.getHeaders().get("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        String token = authHeader.substring(7);
        // TODO: 调用OAuth2授权服务器验证token
        return token != null && !token.isBlank();
    }

    /** HMAC-SHA256签名 */
    public String hmacSha256(String secret, String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC签名失败", e);
        }
    }

    /** 清理过期nonce（定时任务调用） */
    public void cleanExpiredNonces() {
        long now = System.currentTimeMillis();
        nonceCache.entrySet().removeIf(e -> now - e.getValue() > NONCE_TTL_MS);
    }
}
