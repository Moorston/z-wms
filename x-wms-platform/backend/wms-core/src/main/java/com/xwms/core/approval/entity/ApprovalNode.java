package com.xwms.core.approval.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 审批节点 */
@Data
@TableName("wms_approval_node")
public class ApprovalNode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String nodeCode;
    private String nodeName;
    private String processCode;
    private Integer processVersion;

    /** 节点类型: START开始/APPROVAL审批/CC抄送/END结束 */
    private String nodeType;

    /** 节点顺序 */
    private Integer nodeOrder;

    /** 审批类型: ANY任一/ALL全部/MAJORITY多数 */
    private String approveType;

    private String approveRole;
    private String approveUsers;
    private String approveDept;

    /** 超时时间(小时) */
    private Integer timeoutHours;

    /** 超时动作: AUTO_PASS/AUTO_REJECT/ESCALATE/NOTIFY */
    private String timeoutAction;

    private String nextNode;
    private String rejectNode;
    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
