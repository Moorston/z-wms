package com.xwms.common.security.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.xwms.common.security.jwt.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** JWT认证过滤器 从请求头Authorization中提取Token，验证后设置SecurityContext */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 1. 从请求头提取Token
        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token) && !jwtUtil.isRefreshToken(token)) {
            // 2. 解析用户信息
            String username = jwtUtil.getUsernameFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            String roles = jwtUtil.getRolesFromToken(token);

            // 3. 构建权限列表
            List<SimpleGrantedAuthority> authorities = buildAuthorities(roles);

            // 4. 设置认证信息
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 5. 将用户信息放入请求属性，供业务层使用
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentUsername", username);
        }

        filterChain.doFilter(request, response);
    }

    /** 从请求头提取Token */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /** 构建权限列表（角色转Authority） 角色格式：ROLE_ADMIN,ROLE_WAREHOUSE */
    private List<SimpleGrantedAuthority> buildAuthorities(String roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
