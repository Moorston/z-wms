package com.xwms.core.approval.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 审批实例 */
@Data
@TableName("wms_approval_instance")
public class ApprovalInstance {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String instanceId;
    private String processCode;
    private Integer processVersion;
    private String processName;
    private String processType;
    private String warehouseCode;
    private String ownerCode;

    /** 业务类型 */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** 业务数据(JSON) */
    private String bizData;

    /** 审批标题 */
    private String title;

    /** 当前节点 */
    private String currentNode;

    private String currentNodeName;

    /** 状态: PENDING/APPROVING/APPROVED/REJECTED/CANCELLED/TIMEOUT */
    private String status;

    private String submitter;
    private LocalDateTime submitTime;
    private LocalDateTime approveTime;
    private Long durationMs;

    /** 当前审批人 */
    private String currentApprovers;

    /** 已审批人数 */
    private Integer approveCount;

    /** 总审批人数 */
    private Integer totalApprovers;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
