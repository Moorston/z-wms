package com.xwms.common.security.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.PostConstruct;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.xwms.common.exception.BizException;
import com.xwms.common.security.dto.LoginRequest;
import com.xwms.common.security.dto.LoginResponse;
import com.xwms.common.security.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 认证服务 登录/登出/刷新Token 注意：生产环境应从数据库加载用户，此处提供内存用户用于开发测试 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    /**
     * 内存用户存储（开发测试用，生产环境替换为数据库查询） key: username, value: {password, userId, realName, roles,
     * warehouse}
     */
    private final Map<String, Map<String, Object>> users = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化默认用户（admin为跨货主管理员，ownerCode为空表示可忽略租户隔离）
        users.put(
                "admin",
                Map.of(
                        "password", passwordEncoder.encode("admin123"),
                        "userId", 1L,
                        "realName", "系统管理员",
                        "roles", "ADMIN,WAREHOUSE,OPERATION",
                        "warehouse", "WH01",
                        "ownerCode", ""));
        users.put(
                "warehouse",
                Map.of(
                        "password", passwordEncoder.encode("warehouse123"),
                        "userId", 2L,
                        "realName", "仓库管理员",
                        "roles", "WAREHOUSE,OPERATION",
                        "warehouse", "WH01",
                        "ownerCode", "OWNER001"));
        users.put(
                "picker",
                Map.of(
                        "password", passwordEncoder.encode("picker123"),
                        "userId", 3L,
                        "realName", "拣货员",
                        "roles", "OPERATION",
                        "warehouse", "WH01",
                        "ownerCode", "OWNER001"));
    }

    /** 登录 */
    public LoginResponse login(LoginRequest request) {
        // 1. 校验用户
        Map<String, Object> user = users.get(request.getUsername());
        if (user == null) {
            throw new BizException(40101, "用户不存在");
        }

        // 2. 校验密码
        String encodedPassword = (String) user.get("password");
        if (!passwordEncoder.matches(request.getPassword(), encodedPassword)) {
            throw new BizException(40102, "密码错误");
        }

        // 3. 生成Token（含货主和仓库信息，用于多租户隔离）
        Long userId = (Long) user.get("userId");
        String roles = (String) user.get("roles");
        String ownerCode = (String) user.get("ownerCode");
        String warehouse = (String) user.get("warehouse");
        String accessToken =
                jwtUtil.generateAccessToken(
                        request.getUsername(), userId, roles, ownerCode, warehouse);
        String refreshToken = jwtUtil.generateRefreshToken(request.getUsername());

        log.info("用户登录成功: username={}, userId={}", request.getUsername(), userId);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getRemainingSeconds(accessToken))
                .userId(userId)
                .username(request.getUsername())
                .realName((String) user.get("realName"))
                .roles(roles)
                .warehouse((String) user.get("warehouse"))
                .build();
    }

    /** 刷新Token */
    public LoginResponse refreshToken(String refreshToken) {
        // 1. 验证Refresh Token
        if (!jwtUtil.validateToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BizException(40103, "Refresh Token无效或已过期");
        }

        // 2. 获取用户名
        String username = jwtUtil.getUsernameFromToken(refreshToken);
        Map<String, Object> user = users.get(username);
        if (user == null) {
            throw new BizException(40101, "用户不存在");
        }

        // 3. 生成新的Access Token（含货主和仓库信息）
        Long userId = (Long) user.get("userId");
        String roles = (String) user.get("roles");
        String ownerCode = (String) user.get("ownerCode");
        String warehouse = (String) user.get("warehouse");
        String newAccessToken =
                jwtUtil.generateAccessToken(username, userId, roles, ownerCode, warehouse);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Refresh Token继续使用
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getRemainingSeconds(newAccessToken))
                .userId(userId)
                .username(username)
                .realName((String) user.get("realName"))
                .roles(roles)
                .warehouse((String) user.get("warehouse"))
                .build();
    }

    /** 登出（JWT无状态，前端清除Token即可，如需服务端登出需Redis黑名单） */
    public void logout(String token) {
        log.info(
                "用户登出: token={}",
                token != null ? token.substring(0, Math.min(20, token.length())) : "null");
        // 生产环境：将Token加入Redis黑名单，直到过期
    }
}
