package com.xwms.core.datamart.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_data_model")
public class DataModel {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String modelId;
    private String modelName;
    private String modelCode;
    private String modelType;
    private String martId;
    private String description;
    private String modelConfig;
    private String tableName;
    private String fields;
    private String relations;
    private String partitions;
    private String indexes;
    private String storageEngine;
    private String refreshStrategy;
    private String refreshCron;
    private LocalDateTime lastRefreshTime;
    private Long recordCount;
    private BigDecimal dataSizeMb;
    private String status;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
