package com.xwms.core.freeze.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 冻结原因配置 */
@Data
@TableName("wms_freeze_reason")
public class FreezeReason {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 原因编码 */
    private String reasonCode;

    /** 原因名称 */
    private String reasonName;

    /** 冻结类型 */
    private String freezeType;

    /** 是否需要审批: Y/N */
    private String needApprove;

    /** 是否自动解冻: Y/N */
    private String autoUnfreeze;

    /** 自动解冻小时数 */
    private Integer autoUnfreezeHours;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
