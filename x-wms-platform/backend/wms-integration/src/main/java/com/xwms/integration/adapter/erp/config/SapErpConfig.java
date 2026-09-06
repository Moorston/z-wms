package com.xwms.integration.adapter.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * SAP ERP 集成配置
 *
 * <p>所有敏感参数通过 Nacos 配置中心下发，禁止硬编码。
 *
 * <p>典型 Nacos 配置示例（application.yaml）：
 *
 * <pre>
 * integration:
 *   sap:
 *     base-url: https://sap.example.com/sap/opu/odata/sap
 *     username: WMS_INTEGRATION_USER
 *     password: ${KMS:sap-password}  # 生产由密钥管理服务注入
 *     plant: P001
 *     company-code: K001
 *     storage-location: WH01
 *     timeout:
 *       connect: 5000
 *       read: 30000
 *     retry:
 *       max-retries: 3
 *       initial-backoff-ms: 1000
 *       multiplier: 2.0
 *       max-backoff-ms: 30000
 *     csrf-token-ttl-seconds: 1800
 *     enabled: true
 * </pre>
 *
 * <p>Nacos 推送变更后通过 {@link RefreshScope} 自动刷新，无需重启。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "integration.sap")
@RefreshScope
public class SapErpConfig {

    /** SAP OData Gateway 基础 URL，例如 https://sap.example.com/sap/opu/odata/sap */
    private String baseUrl;

    /** 集成服务账号（Basic Auth 用户名） */
    private String username;

    /** 集成服务账号密码（Basic Auth 密码，生产环境由 KMS 注入） */
    private String password;

    /** SAP 工厂代码（默认为所有出入库过账使用） */
    private String plant = "P001";

    /** SAP 公司代码 */
    private String companyCode = "K001";

    /** SAP 库存地点（仓库） */
    private String storageLocation = "WH01";

    /** 是否启用 SAP 集成（false 时所有调用返回降级结果，用于灾备演练） */
    private boolean enabled = true;

    /** HTTP 超时配置 */
    private Timeout timeout = new Timeout();

    /** 重试配置 */
    private Retry retry = new Retry();

    /** CSRF Token 本地缓存有效期（秒），默认 30 分钟 */
    private long csrfTokenTtlSeconds = 1800;

    /** CSRF Token Redis key 前缀 */
    private String csrfKeyPrefix = "sap:csrf:";

    @Data
    public static class Timeout {
        /** 连接超时（毫秒） */
        private int connect = 5000;

        /** 读取超时（毫秒） */
        private int read = 30000;
    }

    @Data
    public static class Retry {
        /** 最大重试次数（不含首次） */
        private int maxRetries = 3;

        /** 初始退避毫秒 */
        private long initialBackoffMs = 1000;

        /** 退避倍数 */
        private double multiplier = 2.0;

        /** 最大退避毫秒 */
        private long maxBackoffMs = 30000;
    }
}
