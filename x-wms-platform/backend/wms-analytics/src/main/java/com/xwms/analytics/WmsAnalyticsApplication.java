package com.xwms.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;

/** WMS 分析服务启动类 端口：8083 职责：KPI报表/经营分析/实时看板/数据归档 数据：ClickHouse OLAP，通过Kafka消费core的库存/订单事件 */
@SpringBootApplication(scanBasePackages = {"com.xwms.analytics", "com.xwms.common"})
@EnableDiscoveryClient
@EnableKafka
public class WmsAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(WmsAnalyticsApplication.class, args);
        System.out.println(
                """
                ╔══════════════════════════════════════╗
                ║   X WMS Analytics Service 启动成功   ║
                ║   Port: 8083  ClickHouse 25.8        ║
                ╚══════════════════════════════════════╝""");
    }
}
