package com.xwms.integration.external.es;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xwms.common.utils.JsonUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch 同步客户端
 *
 * <p>负责：
 *
 * <ul>
 *   <li>批量写入文档（bulk index）
 *   <li>按主键查询单条文档
 *   <li>从文档中提取 ES 主键（logNo / messageId / callbackNo）
 * </ul>
 *
 * <p>使用 elasticsearch-java 8.x REST Client，不依赖 Spring Data ES。
 */
@Slf4j
@Component
public class EsSyncClient {

    @Getter private final ElasticsearchClient client;

    public EsSyncClient(
            @Value("${elasticsearch.host:localhost}") String host,
            @Value("${elasticsearch.port:9200}") int port,
            @Value("${elasticsearch.scheme:http}") String scheme,
            @Value("${elasticsearch.username:}") String username,
            @Value("${elasticsearch.password:}") String password) {
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));
        if (username != null && !username.isBlank()) {
            String basicAuth =
                    "Basic "
                            + Base64.getEncoder()
                                    .encodeToString(
                                            (username + ":" + password)
                                                    .getBytes(StandardCharsets.UTF_8));
            builder.setDefaultHeaders(
                    new Header[] {new BasicHeader("Authorization", basicAuth)});
        }
        RestClient restClient = builder.build();
        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper());
        this.client = new ElasticsearchClient(transport);
        log.info("Elasticsearch 客户端初始化成功: {}:{}", host, port);
    }

    /**
     * 批量写入 ES
     *
     * @param index 索引名
     * @param documents 文档列表
     */
    public void bulkIndex(String index, List<Object> documents) throws Exception {
        BulkRequest.Builder builder = new BulkRequest.Builder();

        for (Object doc : documents) {
            String json = JsonUtils.toJson(doc);
            InputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            String id = extractId(doc);
            builder.operations(op -> op.index(idx -> idx.index(index).id(id).document(in)));
        }

        BulkResponse response = client.bulk(builder.build());
        if (response.errors()) {
            for (var item : response.items()) {
                if (item.error() != null) {
                    log.warn(
                            "ES 批量写入部分失败: index={}, id={}, error={}",
                            index,
                            item.id(),
                            item.error().reason());
                }
            }
        }
        log.debug("ES 批量写入完成: index={}, count={}", index, documents.size());
    }

    /**
     * 按 ID 查询单条文档
     *
     * @param index 索引名
     * @param id 文档主键
     * @return 文档 JSON 字符串，不存在返回 null
     */
    public String getById(String index, String id) throws Exception {
        GetRequest request = GetRequest.of(g -> g.index(index).id(id));
        GetResponse<String> resp = client.get(request, String.class);
        if (!resp.found()) return null;
        return resp.source();
    }

    /**
     * 从文档中提取 ES 主键
     *
     * <p>优先使用 logNo / messageId / callbackNo，否则生成 UUID。 使用业务主键保证幂等——重复事件覆盖写入。
     */
    @SuppressWarnings("unchecked")
    public String extractId(Object doc) {
        if (doc instanceof Map map) {
            String logNo = (String) map.get("logNo");
            if (logNo != null) return logNo;
            String messageId = (String) map.get("messageId");
            if (messageId != null) return messageId;
            String callbackNo = (String) map.get("callbackNo");
            if (callbackNo != null) return callbackNo;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
