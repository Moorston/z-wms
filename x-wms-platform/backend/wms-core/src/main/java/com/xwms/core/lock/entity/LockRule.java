package com.xwms.core.lock.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存锁规则 */
@Data
@TableName("wms_lock_rule")
public class LockRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 锁范围: SKU/LOCATION/BATCH/WAREHOUSE/OWNER */
    private String lockScope;

    /** 锁类型: SHARED/EXCLUSIVE */
    private String lockType;

    /** 最大等待时间(秒) */
    private Integer maxWaitTime;

    /** 超时时间(秒) */
    private Integer timeoutTime;

    /** 重试次数 */
    private Integer retryCount;

    /** 重试间隔(毫秒) */
    private Integer retryInterval;

    /** 是否死锁检测: Y/N */
    private String deadlockDetect;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
