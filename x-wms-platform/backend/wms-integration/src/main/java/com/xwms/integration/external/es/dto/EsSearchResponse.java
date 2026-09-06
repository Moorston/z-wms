package com.xwms.integration.external.es.dto;

import java.util.List;

import lombok.Data;

/** ES 全文检索响应 */
@Data
public class EsSearchResponse<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;

    public EsSearchResponse(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
