package com.xwms.common.security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/** JWT工具类 支持Access Token（2小时）和Refresh Token（7天） 使用HMAC-SHA256签名 */
@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret:x-wms-platform-secret-key-2026-must-be-at-least-256-bits-long}")
    private String secret;

    @Value("${jwt.access-expiration:7200000}")
    private long accessExpiration; // 2小时

    @Value("${jwt.refresh-expiration:604800000}")
    private long refreshExpiration; // 7天

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 生成Access Token（含货主和仓库信息，用于多租户隔离） */
    public String generateAccessToken(
            String username, Long userId, String roles, String ownerCode, String warehouse) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("roles", roles);
        claims.put("type", "access");
        if (ownerCode != null) {
            claims.put("ownerCode", ownerCode);
        }
        if (warehouse != null) {
            claims.put("warehouse", warehouse);
        }
        return buildToken(claims, username, accessExpiration);
    }

    /** 生成Access Token（兼容旧版，不含货主信息） */
    public String generateAccessToken(String username, Long userId, String roles) {
        return generateAccessToken(username, userId, roles, null, null);
    }

    /** 生成Refresh Token */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, username, refreshExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /** 解析Token获取Claims */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /** 从Token中获取用户名 */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /** 从Token中获取用户ID */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return null;
        Object userId = claims.get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    /** 从Token中获取角色 */
    @SuppressWarnings("unchecked")
    public String getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("roles") : null;
    }

    /** 从Token中获取货主编码（多租户隔离） */
    public String getOwnerCodeFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("ownerCode") : null;
    }

    /** 从Token中获取仓库编码 */
    public String getWarehouseFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("warehouse") : null;
    }

    /** 验证Token是否有效 */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) return false;
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    /** 判断是否为Refresh Token */
    public boolean isRefreshToken(String token) {
        Claims claims = parseToken(token);
        return claims != null && "refresh".equals(claims.get("type"));
    }

    /** 获取Token剩余有效期（秒） */
    public long getRemainingSeconds(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return 0;
        long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }
}
