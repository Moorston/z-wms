package com.xwms.core.approval.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 审批记录 */
@Data
@TableName("wms_approval_record")
public class ApprovalRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordId;
    private String instanceId;
    private String nodeCode;
    private String nodeName;
    private Integer nodeOrder;

    private String approver;
    private String approveRole;
    private String approveDept;

    /** 操作: APPROVE通过/REJECT驳回/CC抄送/TRANSFER转办/WITHDRAW撤回 */
    private String action;

    /** 审批意见 */
    private String opinion;

    private LocalDateTime approveTime;
    private Long durationMs;

    private String fromNode;
    private String toNode;
    private String attachmentUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
