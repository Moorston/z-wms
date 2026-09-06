package com.xwms.base;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/** WMS 基础服务启动类 端口：8082 职责：库位/规则引擎/基础数据/月台费收/系统管理 */
@SpringBootApplication(scanBasePackages = {"com.xwms.base", "com.xwms.common"})
@EnableDiscoveryClient
@MapperScan("com.xwms.base.**.mapper")
public class WmsBaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsBaseApplication.class, args);
        System.out.println(
                """
                ╔══════════════════════════════════════╗
                ║   X WMS Base Service 启动成功        ║
                ║   Port: 8082  Java 25  Spring Boot 3.5║
                ╚══════════════════════════════════════╝""");
    }
}
