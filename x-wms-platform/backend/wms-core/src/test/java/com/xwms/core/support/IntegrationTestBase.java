package com.xwms.core.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基类
 * 使用TestContainers启动真实中间件：
 * - MySQL 8.0（通过JDBC URL自动启动，与生产环境一致）
 * - Redis 8.0
 * - Kafka 4.0 KRaft
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("integration")
public abstract class IntegrationTestBase {

    /**
     * Redis容器
     */
    @Container
    protected static final GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:8.0-alpine"))
                    .withExposedPorts(6379)
                    .withReuse(true);

    /**
     * Kafka容器（KRaft模式，无需ZooKeeper）
     */
    @Container
    protected static final KafkaContainer kafkaContainer =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"))
                    .withKraft()
                    .withReuse(true);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry)
    {
        // Redis动态端口
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        // Kafka动态端口
        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
    }

    /**
     * 等待Kafka消费者就绪
     */
    protected void waitForKafka(long millis)
    {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e)
    {
            Thread.currentThread().interrupt();
        }
    }
}
