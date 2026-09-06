package com.xwms.common.tenant.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.xwms.common.security.jwt.JwtUtil;
import com.xwms.common.tenant.context.OwnerContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 货主上下文过滤器 从请求中解析当前货主编码，设置到OwnerContext，请求结束时清除。
 *
 * <p>货主编码获取优先级： 1. 请求头 X-Owner-Code（Feign内部调用透传，优先级最高） 2. JWT Token中的ownerCode claim（外部用户请求） 3.
 * 请求参数 ownerCode（调试/测试用）
 *
 * <p>注意： - 必须在请求结束时清除OwnerContext，否则线程池复用会导致数据串货主 -
 * Filter顺序在TraceIdFilter之后、JwtAuthenticationFilter之后 -
 * 未登录/内部调用无ownerCode时，OwnerContext为空，租户插件自动跳过隔离
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10) // TraceIdFilter是HIGHEST_PRECEDENCE，这里紧随其后
public class OwnerFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    private static final String OWNER_HEADER = "X-Owner-Code";
    private static final String OWNER_PARAM = "ownerCode";
    private static final String AUTH_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. 解析货主编码
            String ownerCode = resolveOwnerCode(request);

            // 2. 设置到上下文和MDC
            if (StringUtils.hasText(ownerCode)) {
                OwnerContext.set(ownerCode);
                MDC.put("ownerCode", ownerCode); // 日志中可通过%X{ownerCode}输出
                log.debug(
                        "OwnerContext设置: ownerCode={}, uri={}", ownerCode, request.getRequestURI());
            }

            // 3. 将ownerCode放入响应头，便于调试
            if (OwnerContext.hasOwner()) {
                response.setHeader(OWNER_HEADER, OwnerContext.get());
            }

            filterChain.doFilter(request, response);
        } finally {
            // 4. 必须清除，防止线程池复用导致数据串货主
            OwnerContext.clear();
            MDC.remove("ownerCode");
        }
    }

    /** 解析货主编码 优先级：请求头 > JWT > 请求参数 */
    private String resolveOwnerCode(HttpServletRequest request) {
        // 1. 请求头（Feign透传，优先级最高）
        String ownerCode = request.getHeader(OWNER_HEADER);
        if (StringUtils.hasText(ownerCode)) {
            return ownerCode;
        }

        // 2. JWT Token
        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token) && !jwtUtil.isRefreshToken(token)) {
            ownerCode = jwtUtil.getOwnerCodeFromToken(token);
            if (StringUtils.hasText(ownerCode)) {
                return ownerCode;
            }
        }

        // 3. 请求参数（调试用）
        ownerCode = request.getParameter(OWNER_PARAM);
        if (StringUtils.hasText(ownerCode)) {
            return ownerCode;
        }

        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }
}
