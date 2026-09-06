package com.xwms.common.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.xwms.common.security.filter.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/** Spring Security配置 - 无状态Session（JWT） - 白名单：登录/健康检查/API文档 - 方法级权限控制（@PreAuthorize） */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /** 白名单路径（无需认证） */
    private static final String[] WHITE_LIST = {
        // 认证接口
        "/api/auth/**",
        // 健康检查
        "/actuator/**",
        "/health",
        // API文档
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/doc.html",
        "/webjars/**",
        // 外部API网关（由API平台自行鉴权）
        "/api/external/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（JWT无状态）
                .csrf(AbstractHttpConfigurer::disable)
                // 无状态Session
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 授权规则
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(WHITE_LIST)
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                // JWT过滤器
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }
}
