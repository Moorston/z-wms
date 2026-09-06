package com.xwms.core.archive.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 归档任务 */
@Data
@TableName("wms_archive_task")
public class ArchiveTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private Long ruleId;
    private String ruleCode;
    private String tableName;
    private String archiveType;

    /** 状态: PENDING/RUNNING/COMPLETED/FAILED/CANCELLED */
    private String status;

    /** 总记录数 */
    private Long totalCount;

    /** 已归档数 */
    private Long archivedCount;

    /** 失败数 */
    private Long failedCount;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 耗时(分钟) */
    private Integer durationMin;

    private String errorMsg;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
