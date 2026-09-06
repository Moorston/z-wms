package com.xwms.core.datamart.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_dimension_define")
public class DimensionDefine {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String dimensionId;
    private String dimensionName;
    private String dimensionCode;
    private String dimensionType;
    private String martId;
    private String description;
    private String dataType;
    private String dataSource;
    private String dimensionConfig;
    private Integer hierarchyLevel;
    private String parentDimensionId;
    private String isTimeDimension;
    private String isGeoDimension;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
