package com.xwms.core;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

/** WMS 核心服务启动类 端口：8081 职责：入库/出库/库存/作业执行/批次/质检/越库调拨 */
@SpringBootApplication(scanBasePackages = {"com.xwms.core", "com.xwms.common"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.xwms")
@EnableKafka
@MapperScan("com.xwms.core.**.mapper")
public class WmsCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsCoreApplication.class, args);
        System.out.println(
                """
                ╔══════════════════════════════════════╗
                ║   X WMS Core Service 启动成功        ║
                ║   Port: 8081  Java 25  Spring Boot 3.5║
                ╚══════════════════════════════════════╝""");
    }
}
