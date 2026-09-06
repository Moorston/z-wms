package com.xwms.core.plugin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 插件配置 */
@Data
@TableName("wms_plugin_config")
public class PluginConfig {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String pluginCode;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置类型: STRING/NUMBER/BOOLEAN/JSON */
    private String configType;

    private String description;

    /** 是否必填 */
    private Integer isRequired;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
