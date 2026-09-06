package com.xwms.core.datamart.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_data_mart")
public class DataMart {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String martId;
    private String martName;
    private String martCode;
    private String martType;
    private String warehouseCode;
    private String ownerCode;
    private String description;
    private String dataSource;
    private String martConfig;
    private String refreshStrategy;
    private String refreshCron;
    private LocalDateTime lastRefreshTime;
    private LocalDateTime nextRefreshTime;
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
