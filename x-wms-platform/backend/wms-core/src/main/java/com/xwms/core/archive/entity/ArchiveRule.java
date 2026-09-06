package com.xwms.core.archive.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 归档规则 */
@Data
@TableName("wms_archive_rule")
public class ArchiveRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 源表名 */
    private String tableName;

    /** 归档表名 */
    private String archiveTable;

    /** 归档类型: DATE/STATUS/ID */
    private String archiveType;

    /** 日期字段 */
    private String dateField;

    /** 状态字段 */
    private String statusField;

    /** 状态值(逗号分隔) */
    private String statusValues;

    /** 保留天数 */
    private Integer retainDays;

    /** 每批处理条数 */
    private Integer batchSize;

    /** 归档模式: MOVE移动/COPY复制/DELETE仅删除 */
    private String archiveMode;

    /** 存储类型: DATABASE/CSV/OSS */
    private String storageType;

    /** OSS存储路径 */
    private String ossPath;

    private Integer enabled;

    /** 定时执行cron */
    private String cronExpression;

    private LocalDateTime lastExecuteTime;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
