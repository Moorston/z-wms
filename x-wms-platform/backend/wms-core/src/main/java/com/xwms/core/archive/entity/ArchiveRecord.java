package com.xwms.core.archive.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 归档记录 */
@Data
@TableName("wms_archive_record")
public class ArchiveRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long batchId;
    private String batchNo;

    /** 源表名 */
    private String sourceTable;

    /** 源记录ID */
    private Long sourceId;

    /** 源业务单号 */
    private String sourceNo;

    /** 归档表 */
    private String archiveTable;

    private LocalDateTime archiveTime;
    private String archiveBy;

    /** 数据摘要 */
    private String dataSummary;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
