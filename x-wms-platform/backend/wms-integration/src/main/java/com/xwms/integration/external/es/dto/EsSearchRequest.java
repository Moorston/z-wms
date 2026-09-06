package com.xwms.integration.external.es.dto;

import lombok.Data;

/** ES 全文检索请求 */
@Data
public class EsSearchRequest {

    /** 全文关键词（模糊匹配所有 text 字段） */
    private String keyword;

    /** 系统编码（精确过滤） */
    private String systemCode;

    /** 状态（精确过滤） */
    private String status;

    /** 业务类型（精确过滤） */
    private String businessType;

    /** 业务单号（精确过滤） */
    private String businessNo;

    /** 方向（精确过滤） */
    private String direction;

    /** 链路追踪 ID（精确过滤） */
    private String traceId;

    /** 起始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 页码（从 0 开始） */
    private int page = 0;

    /** 页大小 */
    private int size = 20;

    /** 排序字段（默认 createdTime） */
    private String sortField = "createdTime";

    /** 排序方向 asc/desc（默认 desc） */
    private String sortOrder = "desc";
}
