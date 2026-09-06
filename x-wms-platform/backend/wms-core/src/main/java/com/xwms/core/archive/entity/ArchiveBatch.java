package com.xwms.core.archive.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 归档批次 */
@Data
@TableName("wms_archive_batch")
public class ArchiveBatch {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String batchNo;
    private Long taskId;
    private String taskNo;

    /** 批次序号 */
    private Integer batchIndex;

    /** 起始ID */
    private Long startId;

    /** 结束ID */
    private Long endId;

    /** 本批记录数 */
    private Integer recordCount;

    /** 状态: PENDING/RUNNING/COMPLETED/FAILED */
    private String status;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String errorMsg;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
