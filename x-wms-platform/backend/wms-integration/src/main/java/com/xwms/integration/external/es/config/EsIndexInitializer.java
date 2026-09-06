package com.xwms.integration.external.es.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.xwms.integration.external.es.EsSyncClient;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 索引初始化器
 *
 * <p>在应用启动时创建 3 个业务索引，包含字段映射和 ik_smart 分析器配置。 仅在 elasticsearch.init.enabled=true 时生效，避免开发环境误建索引。
 *
 * <p>索引列表：
 *
 * <ul>
 *   <li>wms-api-call-log — 接口调用日志
 *   <li>wms-integration-message — 集成消息
 *   <li>wms-callback-record — 回调记录
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "elasticsearch.init.enabled",
        havingValue = "true",
        matchIfMissing = false)
public class EsIndexInitializer {

    private final EsSyncClient esSyncClient;

    public EsIndexInitializer(EsSyncClient esSyncClient) {
        this.esSyncClient = esSyncClient;
        initIndexes();
    }

    // ============================================================
    // 索引初始化
    // ============================================================

    private void initIndexes() {
        try {
            ElasticsearchClient client = esSyncClient.getClient();

            createIndexIfNotExists(client, "wms-api-call-log", buildApiCallLogMapping());
            createIndexIfNotExists(
                    client, "wms-integration-message", buildIntegrationMessageMapping());
            createIndexIfNotExists(client, "wms-callback-record", buildCallbackRecordMapping());

            log.info("Elasticsearch 索引初始化完成");
        } catch (Exception e) {
            log.warn("Elasticsearch 索引初始化失败（索引将由首次写入自动创建）: {}", e.getMessage());
        }
    }

    private void createIndexIfNotExists(
            ElasticsearchClient client, String indexName, Map<String, Property> properties)
            throws Exception {
        if (client.indices().exists(e -> e.index(indexName)).value()) {
            log.debug("索引已存在，跳过创建: {}", indexName);
            return;
        }

        TypeMapping mappings = buildMappings(properties);

        CreateIndexRequest request =
                CreateIndexRequest.of(
                        r ->
                                r.index(indexName)
                                        .settings(
                                                s ->
                                                        s.numberOfShards("1")
                                                                .numberOfReplicas("1"))
                                        .mappings(mappings));

        CreateIndexResponse resp = client.indices().create(request);
        if (resp.acknowledged()) {
            log.info("Elasticsearch 索引创建成功: {}", indexName);
        } else {
            log.warn("Elasticsearch 索引创建未确认: {}", indexName);
        }
    }

    private TypeMapping buildMappings(Map<String, Property> properties) {
        return TypeMapping.of(m -> m.properties(properties));
    }

    // ============================================================
    // 字段映射：API 调用日志
    // ============================================================

    private Map<String, Property> buildApiCallLogMapping() {
        return mapOf(
                kv("logNo", keyword()),
                kv("systemCode", keyword()),
                kv("systemName", keyword()),
                kv("apiName", textIk()),
                kv("apiUrl", textIk()),
                kv("httpMethod", keyword()),
                kv("requestBody", textIk()),
                kv("responseBody", textIk()),
                kv("requestHeaders", textIk()),
                kv("responseStatus", integer()),
                kv("costTime", longType()),
                kv("status", keyword()),
                kv("errorMsg", textIk()),
                kv("retryCount", integer()),
                kv("traceId", keyword()),
                kv("businessType", keyword()),
                kv("businessNo", textIk()),
                kv("direction", keyword()),
                kv("createdTime", date()));
    }

    // ============================================================
    // 字段映射：集成消息
    // ============================================================

    private Map<String, Property> buildIntegrationMessageMapping() {
        return mapOf(
                kv("messageId", keyword()),
                kv("systemCode", keyword()),
                kv("messageType", keyword()),
                kv("topic", keyword()),
                kv("payload", textIk()),
                kv("status", keyword()),
                kv("retryCount", integer()),
                kv("maxRetry", integer()),
                kv("nextRetryTime", date()),
                kv("sentTime", date()),
                kv("consumedTime", date()),
                kv("errorMsg", textIk()),
                kv("businessType", keyword()),
                kv("businessNo", textIk()),
                kv("direction", keyword()),
                kv("createdTime", date()),
                kv("updatedTime", date()));
    }

    // ============================================================
    // 字段映射：回调记录
    // ============================================================

    private Map<String, Property> buildCallbackRecordMapping() {
        return mapOf(
                kv("callbackNo", keyword()),
                kv("systemCode", keyword()),
                kv("callbackUrl", textIk()),
                kv("callbackType", keyword()),
                kv("requestBody", textIk()),
                kv("responseStatus", integer()),
                kv("responseBody", textIk()),
                kv("status", keyword()),
                kv("retryCount", integer()),
                kv("maxRetry", integer()),
                kv("nextRetryTime", date()),
                kv("costTime", longType()),
                kv("errorMsg", textIk()),
                kv("businessType", keyword()),
                kv("businessNo", textIk()),
                kv("createdTime", date()),
                kv("updatedTime", date()));
    }

    // ============================================================
    // 字段类型工厂
    // ============================================================

    private Property keyword() {
        return Property.of(p -> p.keyword(k -> k));
    }

    private Property textIk() {
        return Property.of(p -> p.text(t -> t.analyzer("ik_smart")));
    }

    private Property integer() {
        return Property.of(p -> p.integer(i -> i));
    }

    private Property longType() {
        return Property.of(p -> p.long_(l -> l));
    }

    private Property date() {
        return Property.of(p -> p.date(d -> d.format("yyyy-MM-dd'T'HH:mm:ss")));
    }

    private static Map.Entry<String, Property> kv(String name, Property prop) {
        return Map.entry(name, prop);
    }

    @SafeVarargs
    private static Map<String, Property> mapOf(Map.Entry<String, Property>... entries) {
        Map<String, Property> map = new LinkedHashMap<>(entries.length);
        for (Map.Entry<String, Property> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}
