package com.xwms.core.dashboard.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

@Data
@TableName("wms_visual_component")
public class VisualComponent {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String componentId;
    private String componentName;
    private String componentCode;
    private String componentType;
    private String description;
    private String componentConfig;
    private String dataConfig;
    private String styleConfig;
    private String interactionConfig;
    private String isBuiltin;
    private String isActive;
    private Integer sortOrder;
    private String createdBy;
    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
