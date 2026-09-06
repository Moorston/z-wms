package com.xwms.common.security.controller;

import org.springframework.web.bind.annotation.*;

import com.xwms.common.core.Result;
import com.xwms.common.security.dto.LoginRequest;
import com.xwms.common.security.dto.LoginResponse;
import com.xwms.common.security.dto.RefreshTokenRequest;
import com.xwms.common.security.service.AuthService;

import lombok.RequiredArgsConstructor;

/** 认证Controller 登录/刷新Token/登出 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /** 刷新Token */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return Result.success(authService.refreshToken(request.getRefreshToken()));
    }

    /** 登出 */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && token.startsWith("Bearer ")) {
            authService.logout(token.substring(7));
        }
        return Result.success();
    }
}
