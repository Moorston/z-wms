package com.xwms.base.rule.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 规则执行日志 */
@Data
@TableName("wms_rule_exec_log")
public class RuleExecLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String execNo;
    private String ruleCode;
    private String ruleType;

    /** 业务单号 */
    private String bizNo;

    /** 业务类型 */
    private String bizType;

    /** 输入数据(JSON) */
    private String inputData;

    /** 输出数据(JSON) */
    private String outputData;

    /** 执行结果: SUCCESS/FAIL/SKIP */
    private String execResult;

    private String errorMsg;

    /** 执行耗时(ms) */
    private Long execDuration;

    private String operator;
    private LocalDateTime execTime;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
