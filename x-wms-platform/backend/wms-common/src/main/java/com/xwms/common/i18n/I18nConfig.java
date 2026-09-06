package com.xwms.common.i18n;

import java.util.List;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/** 国际化配置 - 基于Accept-Language请求头的语言解析 - 支持通过?lang=zh_CN参数切换语言 - 消息文件热加载（开发环境） */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    /** 消息源配置 消息文件位置：classpath:i18n/messages 编码：UTF-8 缓存：3600秒（生产环境可加长） */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource =
                new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setCacheSeconds(3600);
        messageSource.setUseCodeAsDefaultMessage(true); // 找不到消息时返回code本身
        return messageSource;
    }

    /** 语言解析器：基于Accept-Language请求头 默认语言：简体中文 */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        resolver.setSupportedLocales(List.of(Locale.SIMPLIFIED_CHINESE, Locale.US, Locale.JAPAN));
        return resolver;
    }

    /** 语言切换拦截器 支持通过请求参数 ?lang=zh_CN 或 ?lang=en_US 切换语言 */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
