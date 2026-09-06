package com.xwms.core.plugin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 插件注册 */
@Data
@TableName("wms_plugin")
public class Plugin {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String pluginCode;
    private String pluginName;

    /** 插件类型: INDUSTRY行业/BUSINESS业务/TECH技术 */
    private String pluginType;

    /** 行业: GSP医药/COLD_CHAIN冷链/ECOMMERCE电商/MANUFACTURING制造 */
    private String industry;

    private String version;
    private String description;

    /** 插件主类 */
    private String pluginClass;

    /** 入口点 */
    private String entryPoint;

    /** 优先级, 数字越小优先级越高 */
    private Integer priority;

    /** 状态: INSTALLED/ENABLED/DISABLED/ERROR */
    private String status;

    /** 配置Schema(JSON) */
    private String configSchema;

    /** 依赖插件(JSON数组) */
    private String dependencies;

    private String author;
    private LocalDateTime installedTime;
    private LocalDateTime enabledTime;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
