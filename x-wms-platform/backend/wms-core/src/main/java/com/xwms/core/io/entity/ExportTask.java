package com.xwms.core.io.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 导出任务 */
@Data
@TableName("wms_export_task")
public class ExportTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskId;
    private String taskName;

    /** 导出类型: INVENTORY/PRODUCT/LOCATION/OWNER/ORDER/BATCH/REPORT/TRANSACTION/KPI */
    private String exportType;

    private String warehouseCode;
    private String ownerCode;
    private String queryParams;
    private String exportColumns;
    private String fileName;
    private String filePath;
    private Long fileSize;

    /** 文件格式: EXCEL/CSV/PDF */
    private String fileFormat;

    private Integer totalCount;
    private Integer exportedCount;

    /** 状态: PENDING/PROCESSING/COMPLETED/FAILED/CANCELLED */
    private String status;

    private String errorMessage;
    private String operator;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private LocalDateTime expireTime;
    private Integer downloadCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
