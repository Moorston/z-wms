package com.xwms.core.io.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 导出记录 */
@Data
@TableName("wms_export_record")
public class ExportRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordId;
    private String taskId;
    private Integer rowNo;
    private String rowData;

    /** 导出状态: PENDING/EXPORTED/FAILED */
    private String exportStatus;

    private String errorMessage;
    private String bizKey;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
