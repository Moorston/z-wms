package com.xwms.common.excel.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/** 导入结果DTO 记录导入成功/失败数量和错误详情 */
@Data
public class ImportResult<T> {
    /** 总行数 */
    private int totalCount;

    /** 成功行数 */
    private int successCount;

    /** 失败行数 */
    private int failCount;

    /** 错误列表 */
    private List<ImportError> errors = new ArrayList<>();

    /** 成功数据列表（可选，用于预览） */
    private List<T> successData = new ArrayList<>();

    /** 导入耗时（毫秒） */
    private long costMillis;

    public void addSuccess(T data) {
        successCount++;
        successData.add(data);
    }

    public void addError(int rowNum, String message) {
        failCount++;
        errors.add(new ImportError(rowNum, message));
    }

    public void addError(int rowNum, String field, String message) {
        failCount++;
        errors.add(new ImportError(rowNum, field, message));
    }

    public boolean hasError() {
        return failCount > 0;
    }
}
