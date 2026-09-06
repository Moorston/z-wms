package com.xwms.common.core;

import lombok.Data;

/** 分页查询参数 */
@Data
public class PageQuery {
    /** 页码（从1开始） */
    private Integer pageNum = 1;

    /** 每页大小 */
    private Integer pageSize = 20;

    /** 排序字段 */
    private String orderBy;

    /** 排序方向：asc/desc */
    private String orderDir = "desc";

    /** 搜索关键字 */
    private String keyword;

    public int getOffset() {
        return (pageNum - 1) * pageSize;
    }
}
