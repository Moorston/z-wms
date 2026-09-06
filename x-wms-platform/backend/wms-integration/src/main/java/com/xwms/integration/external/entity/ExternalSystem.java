package com.xwms.integration.external.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 外部系统配置 */
@Data
@TableName("wms_external_system")
public class ExternalSystem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String systemCode;
    private String systemName;

    /** 系统类型: WCS/TMS/ERP/OMS/CRM/BI/OTHER */
    private String systemType;

    private String baseUrl;
    private String apiKey;
    private String apiSecret;

    /** 认证类型: NONE/BASIC/BEARER/API_KEY/OAUTH2 */
    private String authType;

    /** 超时(ms) */
    private Integer timeout;

    /** 重试次数 */
    private Integer retryCount;

    /** 重试间隔(ms) */
    private Integer retryInterval;

    /** 状态: ACTIVE/INACTIVE/MAINTENANCE */
    private String status;

    private String description;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
