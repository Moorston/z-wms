package com.xwms.common.feign.decoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.xwms.common.core.Result;
import com.xwms.common.exception.BizException;
import com.xwms.common.utils.JsonUtils;

import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/** Feign统一异常解码器 将下游服务的错误响应统一转换为BizException 支持解析Result格式的错误响应 */
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        // 尝试解析响应体为Result格式
        try {
            if (response.body() != null) {
                String body = Util.toString(response.body().asReader(StandardCharsets.UTF_8));
                Result<?> result = JsonUtils.parseObject(body, Result.class);
                if (result != null && result.getCode() != null && result.getCode() != 200) {
                    log.error(
                            "Feign调用失败: method={}, status={}, code={}, msg={}",
                            methodKey,
                            response.status(),
                            result.getCode(),
                            result.getMessage());
                    return new BizException(result.getCode(), result.getMessage());
                }
            }
        } catch (IOException e) {
            log.error("解析Feign错误响应失败: method={}", methodKey, e);
        }

        // HTTP状态码处理
        return switch (response.status()) {
            case 400 -> new BizException(400, "请求参数错误: " + methodKey);
            case 401 -> new BizException(401, "未授权: " + methodKey);
            case 403 -> new BizException(403, "无权限: " + methodKey);
            case 404 -> new BizException(404, "服务不存在: " + methodKey);
            case 500 -> new BizException(500, "下游服务异常: " + methodKey);
            case 503 -> new BizException(503, "服务不可用: " + methodKey);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
