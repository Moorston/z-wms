package com.xwms.common.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;

/**
 * Knife4j + SpringDoc OpenAPI 3.0 接口文档配置
 *
 * <p>访问地址： - Knife4j UI: http://host:port/doc.html - Swagger UI: http://host:port/swagger-ui.html -
 * OpenAPI JSON: http://host:port/v3/api-docs
 *
 * <p>功能特性： 1. 全局JWT Bearer认证（Authorize按钮输入Token） 2. 按服务名称定制文档标题 3. 多环境Server配置 4.
 * 全局响应头（TraceId/OwnerCode）
 */
@Slf4j
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Value("${spring.application.name:x-wms-service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${knife4j.setting.language:zh_cn}")
    private String language;

    @Bean
    public OpenAPI customOpenAPI() {
        // 服务名称转中文标题
        String title = resolveTitle(applicationName);
        String description = resolveDescription(applicationName);

        OpenAPI openAPI =
                new OpenAPI()
                        .info(
                                new Info()
                                        .title(title)
                                        .description(
                                                description
                                                        + "\n\n"
                                                        + "## 认证说明\n"
                                                        + "1. 调用 `/api/auth/login` 获取Access Token\n"
                                                        + "2. 点击右上角 Authorize 按钮，输入 `Bearer {token}`\n"
                                                        + "3. 后续请求自动携带 Authorization 请求头\n\n"
                                                        + "## 多租户说明\n"
                                                        + "- 货主隔离：通过JWT中的ownerCode自动隔离数据\n"
                                                        + "- 内部调用：通过X-Owner-Code请求头透传货主\n"
                                                        + "- 链路追踪：响应头返回X-Trace-Id")
                                        .version("1.0.0")
                                        .contact(
                                                new Contact()
                                                        .name("X WMS Platform")
                                                        .email("admin@xwms.com")
                                                        .url(
                                                                "https://github.com/Moorston/x-wms-platform"))
                                        .license(
                                                new License()
                                                        .name("Apache 2.0")
                                                        .url(
                                                                "https://www.apache.org/licenses/LICENSE-2.0")))
                        // 全局JWT认证
                        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                        .components(
                                new Components()
                                        .addSecuritySchemes(
                                                SECURITY_SCHEME_NAME,
                                                new SecurityScheme()
                                                        .name(SECURITY_SCHEME_NAME)
                                                        .type(SecurityScheme.Type.HTTP)
                                                        .scheme("bearer")
                                                        .bearerFormat("JWT")
                                                        .in(SecurityScheme.In.HEADER)
                                                        .description(
                                                                "JWT Bearer Token，格式：Bearer {access_token}")))
                        .servers(
                                List.of(
                                        new Server()
                                                .url("http://localhost:" + serverPort)
                                                .description("本地开发环境"),
                                        new Server()
                                                .url("http://wms-gateway:9080")
                                                .description("测试环境"),
                                        new Server()
                                                .url("https://api.xwms.com")
                                                .description("生产环境")));

        log.info("Knife4j OpenAPI文档已初始化: title={}, port={}", title, serverPort);
        return openAPI;
    }

    /** 根据服务名称解析中文标题 */
    private String resolveTitle(String appName) {
        return switch (appName) {
            case "wms-core" -> "X WMS 核心服务 API文档";
            case "wms-base" -> "X WMS 基础数据服务 API文档";
            case "wms-analytics" -> "X WMS 分析报表服务 API文档";
            case "wms-integration" -> "X WMS 集成平台服务 API文档";
            default -> "X WMS Platform API文档";
        };
    }

    /** 根据服务名称解析描述 */
    private String resolveDescription(String appName) {
        return switch (appName) {
            case "wms-core" -> "核心业务服务：入库管理、出库管理、库存管理、批次管理、作业管理、波次管理";
            case "wms-base" -> "基础数据服务：仓库/库区/库位管理、商品/货主/客户档案、规则引擎、系统管理";
            case "wms-analytics" -> "分析报表服务：KPI计算、OLAP查询、报表生成、数据同步";
            case "wms-integration" -> "集成平台服务：API网关、外部系统适配器、快递单管理、异步任务";
            default -> "X WMS企业级仓储管理平台";
        };
    }
}
