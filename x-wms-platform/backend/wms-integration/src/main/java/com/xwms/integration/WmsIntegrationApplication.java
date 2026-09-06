package com.xwms.integration;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

/** WMS 集成服务启动类 端口：8084 职责：ERP/TMS/WCS/电商/快递适配器+API接口服务平台 核心：外部系统故障隔离，熔断降级，不影响核心作业 */
@SpringBootApplication(scanBasePackages = {"com.xwms.integration", "com.xwms.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.xwms")
@EnableKafka
@MapperScan("com.xwms.integration.**.mapper")
public class WmsIntegrationApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsIntegrationApplication.class, args);
        System.out.println(
                """
                ╔══════════════════════════════════════╗
                ║   X WMS Integration Service 启动成功 ║
                ║   Port: 8084  API平台+适配器         ║
                ╚══════════════════════════════════════╝""");
    }
}
