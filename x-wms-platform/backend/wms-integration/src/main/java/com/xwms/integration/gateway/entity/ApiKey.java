package com.xwms.integration.gateway.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** API密钥 */
@Data
@TableName("wms_api_key")
public class ApiKey {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String appKey;
    private String appSecret;
    private String appName;

    /** 应用类型: ERP/TMS/WCS/THIRD_PARTY/INTERNAL */
    private String appType;

    private String description;

    /** 全局限流(QPS) */
    private Integer rateLimit;

    /** IP白名单(JSON数组) */
    private String ipWhitelist;

    /** 状态: ACTIVE/DISABLED/EXPIRED */
    private String status;

    private LocalDateTime expireTime;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
