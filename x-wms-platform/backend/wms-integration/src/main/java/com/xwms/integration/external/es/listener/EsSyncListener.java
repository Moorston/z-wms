package com.xwms.integration.external.es.listener;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.xwms.integration.external.es.EsSyncClient;
import com.xwms.integration.external.es.event.EsSyncEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ES 同步监听器
 *
 * <p>异步接收 {@link EsSyncEvent}，批量写入 ES。 失败不抛异常——ES 同步是最终一致性，失败后由补偿 Job 兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsSyncListener {

    private final EsSyncClient esSyncClient;

    @Async("esSyncExecutor")
    @EventListener
    public void onEsSyncEvent(EsSyncEvent event) {
        try {
            esSyncClient.bulkIndex(event.getIndex(), event.getDocuments());
        } catch (Exception e) {
            log.error(
                    "ES 同步失败: index={}, count={}",
                    event.getIndex(),
                    event.getDocuments().size(),
                    e);
            // 不抛异常——ES 同步是最终一致性，失败后由补偿 Job 兜底
        }
    }
}
