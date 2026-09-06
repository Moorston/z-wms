package com.xwms.common.core;

import java.util.List;

import lombok.Data;

/** 分页结果 */
@Data
public class PageResult<T> {
    /** 当前页数据 */
    private List<T> records;

    /** 总记录数 */
    private Long total;

    /** 当前页码 */
    private Integer pageNum;

    /** 每页大小 */
    private Integer pageSize;

    /** 总页数 */
    private Long pages;

    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        PageResult<T> r = new PageResult<>();
        r.setRecords(records);
        r.setTotal(total);
        r.setPageNum(pageNum);
        r.setPageSize(pageSize);
        r.setPages(pageSize > 0 ? (total + pageSize - 1) / pageSize : 0);
        return r;
    }
}
