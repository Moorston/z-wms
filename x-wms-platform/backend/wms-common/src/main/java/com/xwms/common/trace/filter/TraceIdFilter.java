package com.xwms.common.trace.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.xwms.common.trace.util.TraceIdUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * TraceId过滤器 1. 从请求头X-Trace-Id获取TraceId（上游传递） 2. 如果没有则生成新的TraceId 3. 设置到MDC，日志输出自动包含 4.
 * 响应头返回TraceId（方便前端排查） 5. 请求结束清除MDC
 *
 * <p>优先级最高，确保所有后续处理都有TraceId
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startTime = System.currentTimeMillis();
        try {
            // 1. 从请求头获取或生成TraceId
            String traceIdFromHeader = request.getHeader(TraceIdUtil.TRACE_ID_HEADER);
            String traceId = TraceIdUtil.initTraceId(traceIdFromHeader);

            // 2. 响应头返回TraceId
            response.setHeader(TraceIdUtil.TRACE_ID_HEADER, traceId);

            // 3. 记录请求入口日志
            log.info(
                    "请求开始: method={}, uri={}, traceId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    traceId);

            // 4. 继续执行
            filterChain.doFilter(request, response);

        } finally {
            // 5. 请求耗时
            long cost = System.currentTimeMillis() - startTime;
            log.info("请求结束: uri={}, cost={}ms", request.getRequestURI(), cost);

            // 6. 清除MDC（关键！防止线程池复用导致TraceId串号）
            TraceIdUtil.clear();
        }
    }
}
