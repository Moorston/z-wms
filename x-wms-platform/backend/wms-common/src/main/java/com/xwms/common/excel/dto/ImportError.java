package com.xwms.common.excel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 导入错误信息 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {
    /** 行号（从1开始，含表头） */
    private int rowNum;

    /** 字段名 */
    private String field;

    /** 错误信息 */
    private String message;

    public ImportError(int rowNum, String message) {
        this.rowNum = rowNum;
        this.message = message;
    }
}
