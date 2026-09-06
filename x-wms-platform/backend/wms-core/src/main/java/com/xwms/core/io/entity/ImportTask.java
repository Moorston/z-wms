package com.xwms.core.io.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 导入任务 */
@Data
@TableName("wms_import_task")
public class ImportTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskId;
    private String taskName;

    /** 导入类型: INVENTORY/PRODUCT/LOCATION/OWNER/ORDER/BATCH/ADJUST/SERIAL */
    private String importType;

    private String warehouseCode;
    private String ownerCode;
    private String fileName;
    private String filePath;
    private Long fileSize;

    /** 文件格式: EXCEL/CSV */
    private String fileFormat;

    private Integer totalCount;
    private Integer successCount;
    private Integer failCount;
    private Integer skipCount;

    /** 状态: PENDING/PROCESSING/COMPLETED/FAILED/CANCELLED */
    private String status;

    private String errorMessage;
    private String operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
