package com.xwms.integration.external.es.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

/**
 * ES 同步事件
 *
 * <p>MySQL 写入成功后发布此事件，由 {@code EsSyncListener} 异步投递到 ES。
 */
public class EsSyncEvent extends ApplicationEvent {

    private final List<Object> documents;
    private final String index;

    public EsSyncEvent(Object source, List<Object> documents, String index) {
        super(source);
        this.documents = documents;
        this.index = index;
    }

    public List<Object> getDocuments() {
        return documents;
    }

    public String getIndex() {
        return index;
    }
}
