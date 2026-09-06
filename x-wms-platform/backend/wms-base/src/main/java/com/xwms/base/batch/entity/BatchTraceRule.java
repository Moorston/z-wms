package com.xwms.base.batch.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 批次追踪规则 */
@Data
@TableName("wms_batch_trace_rule")
public class BatchTraceRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 规则类型: INBOUND/OUTBOUND/TRANSFER/ADJUST/QC */
    private String ruleType;

    /** 触发事件 */
    private String triggerEvent;

    /** 追踪字段(JSON数组) */
    private String traceFields;

    /** 追踪深度(正向/反向) */
    private Integer traceDepth;

    private Integer enabled;
    private String description;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
