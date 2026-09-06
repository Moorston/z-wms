package com.xwms.core.io.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 导入记录 */
@Data
@TableName("wms_import_record")
public class ImportRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordId;
    private String taskId;
    private Integer rowNo;
    private String rowData;

    /** 导入状态: PENDING/SUCCESS/FAILED/SKIPPED */
    private String importStatus;

    private String errorCode;
    private String errorMessage;
    private String bizKey;
    private Integer retryCount;
    private String operator;
    private LocalDateTime processTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
