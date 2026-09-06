package com.xwms.integration.gateway.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** API定义 */
@Data
@TableName("wms_api_definition")
public class ApiDefinition {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String apiCode;
    private String apiName;
    private String apiPath;

    /** 请求方法: GET/POST/PUT/DELETE */
    private String apiMethod;

    /** API分类 */
    private String apiCategory;

    private String description;

    /** 请求参数(JSON Schema) */
    private String requestParams;

    /** 响应结构(JSON Schema) */
    private String responseSchema;

    /** 是否需要鉴权 */
    private Integer authRequired;

    /** 限流(QPS) */
    private Integer rateLimit;

    /** 超时(秒) */
    private Integer timeout;

    private Integer enabled;
    private String version;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
