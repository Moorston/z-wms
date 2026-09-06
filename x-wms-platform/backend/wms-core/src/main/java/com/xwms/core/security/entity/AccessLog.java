package com.xwms.core.security.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 访问日志 */
@Data
@TableName("wms_access_log")
public class AccessLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logId;
    private String traceId;
    private String userCode;
    private String userName;
    private String warehouseCode;

    /** 访问类型: LOGIN/LOGOUT/API/PAGE/DOWNLOAD/UPLOAD */
    private String accessType;

    private String accessUrl;
    private String accessMethod;
    private String accessParams;
    private String ipAddress;
    private String userAgent;

    /** 设备类型: PC/PDA/PACK_STATION */
    private String deviceType;

    private Integer statusCode;
    private Long responseTime;

    /** 是否成功: Y/N */
    private String isSuccess;

    private String errorMessage;
    private String sessionId;
    private String tokenId;
    private LocalDateTime accessTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
