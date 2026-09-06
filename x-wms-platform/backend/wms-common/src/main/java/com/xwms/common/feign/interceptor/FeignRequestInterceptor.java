package com.xwms.common.feign.interceptor;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.xwms.common.tenant.context.OwnerContext;
import com.xwms.common.trace.util.TraceIdUtil;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Feign请求拦截器 核心能力： 1. TraceId透传（MDC中的traceId传递到下游，实现全链路追踪） 2. 认证头传递（JWT Token从上游请求透传到下游） 3.
 * 灰度标记传递（灰度发布时的流量标记） 4. 货主编码透传（多租户隔离，OwnerContext → X-Owner-Code请求头）
 */
@Configuration
public class FeignRequestInterceptor implements RequestInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String GRAY_HEADER = "X-Gray-Flag";
    private static final String OWNER_HEADER = "X-Owner-Code";

    @Override
    public void apply(RequestTemplate template) {
        // 1. TraceId透传（从MDC取，由TraceIdFilter设置）
        String traceId = TraceIdUtil.getTraceId();
        if (traceId != null) {
            template.header(TraceIdUtil.TRACE_ID_HEADER, traceId);
        }
        String spanId = TraceIdUtil.getSpanId();
        if (spanId != null) {
            template.header(TraceIdUtil.SPAN_ID_HEADER, spanId);
        }

        // 2. 认证头透传（JWT Token）
        String authHeader = getHeaderFromRequest(AUTH_HEADER);
        if (authHeader != null) {
            template.header(AUTH_HEADER, authHeader);
        }

        // 3. 灰度标记透传
        String grayFlag = getHeaderFromRequest(GRAY_HEADER);
        if (grayFlag != null) {
            template.header(GRAY_HEADER, grayFlag);
        }

        // 4. 货主编码透传（多租户隔离）
        // 优先从OwnerContext取（当前线程的货主上下文），其次从请求头取
        String ownerCode = OwnerContext.get();
        if (ownerCode == null) {
            ownerCode = getHeaderFromRequest(OWNER_HEADER);
        }
        if (ownerCode != null) {
            template.header(OWNER_HEADER, ownerCode);
        }

        // 5. 服务来源标记（用于日志区分调用来源）
        template.header("X-Service-From", "wms-service");
    }

    /** 从当前HTTP请求中获取指定Header */
    private String getHeaderFromRequest(String headerName) {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader(headerName);
            }
        } catch (Exception e) {
            // 非Web上下文（如异步线程/Kafka消费者），忽略
        }
        return null;
    }
}
