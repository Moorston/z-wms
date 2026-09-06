package com.xwms.core.dashboard.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_data_board")
public class DataBoard {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String boardId;
    private String boardName;
    private String boardCode;
    private String boardType;
    private String warehouseCode;
    private String ownerCode;
    private String description;
    private String boardConfig;
    private String dataSource;
    private String chartConfig;
    private Integer refreshInterval;
    private String periodType;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
