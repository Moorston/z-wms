package com.xwms.base.audit.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 操作日志 */
@Data
@TableName("sys_operation_log")
public class OperationLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String userId;
    private String userName;

    /** 模块: 入库/出库/库存/系统等 */
    private String module;

    /** 操作: 创建/修改/删除/审核等 */
    private String operation;

    private String method;
    private String requestUrl;
    private String requestMethod;
    private String requestParams;
    private String responseResult;
    private String ipAddress;
    private String userAgent;

    /** 耗时(ms) */
    private Long costTime;

    /** 状态: SUCCESS/FAILED */
    private String status;

    private String errorMsg;

    /** 业务类型 */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    /** 链路追踪ID */
    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
