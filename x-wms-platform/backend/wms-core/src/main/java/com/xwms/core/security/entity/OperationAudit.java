package com.xwms.core.security.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 操作审计 */
@Data
@TableName("wms_operation_audit")
public class OperationAudit {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String auditId;
    private String traceId;
    private String userCode;
    private String userName;
    private String roleCode;
    private String warehouseCode;
    private String ownerCode;

    /** 模块: INVENTORY/INBOUND/OUTBOUND等 */
    private String module;

    /** 操作: CREATE/UPDATE/DELETE/APPROVE/EXPORT等 */
    private String operation;

    /** 操作类型: QUERY/ADD/EDIT/DELETE/APPROVE/EXPORT/IMPORT/LOGIN/LOGOUT/PRINT/AUDIT */
    private String operationType;

    private String bizType;
    private String bizNo;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String responseData;
    private String beforeData;
    private String afterData;
    private String ipAddress;
    private String userAgent;

    /** 设备类型: PC/PDA/PACK_STATION */
    private String deviceType;

    /** 状态: SUCCESS/FAILED */
    private String status;

    private String errorMessage;
    private Long durationMs;
    private LocalDateTime operationTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
