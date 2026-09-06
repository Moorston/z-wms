package com.xwms.common.feign.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.Request;
import feign.Retryer;

/**
 * Feign全局配置 - 超时：连接5s/读取10s - 重试：默认不重试（业务接口幂等性难保证，由调用方控制） - 日志：BASIC级别（生产环境可关） -
 * 拦截器：TraceId透传+认证头传递
 */
@Configuration
public class FeignConfig {

    /** 超时配置 connectTimeout: 5s（建立连接） readTimeout: 10s（读取响应） */
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                5, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, true // 跟随重定向
                );
    }

    /** 重试策略：默认不重试 WMS业务接口多为写操作，自动重试可能导致重复扣减/重复创建 如需重试，在具体FeignClient上单独配置 */
    @Bean
    public Retryer feignRetryer() {
        return Retryer.NEVER_RETRY;
    }

    /** 日志级别：BASIC（仅记录请求方法、URL、响应状态、耗时） 生产环境建议NONE，排查问题时临时调FULL */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
