package com.xwms.core.plugin.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 插件执行日志 */
@Data
@TableName("wms_plugin_log")
public class PluginLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logNo;
    private String pluginCode;
    private String pluginName;

    /** 业务类型: INBOUND/OUTBOUND/INVENTORY等 */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    /** 触发点 */
    private String triggerPoint;

    private String inputData;
    private String outputData;

    /** 状态: SUCCESS/FAILED/SKIPPED */
    private String status;

    private String errorMsg;

    /** 耗时(ms) */
    private Long costTime;

    private String traceId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
