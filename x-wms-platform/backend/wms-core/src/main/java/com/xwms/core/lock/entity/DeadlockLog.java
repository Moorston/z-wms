package com.xwms.core.lock.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 死锁检测日志 */
@Data
@TableName("wms_deadlock_log")
public class DeadlockLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String logId;
    private LocalDateTime detectTime;

    /** 涉及锁键 */
    private String lockKey;

    /** 涉及的锁(JSON) */
    private String involvedLocks;

    /** 涉及持有者(JSON) */
    private String involvedHolders;

    /** 被牺牲的锁ID */
    private String victimLockId;

    /** 被牺牲的持有者 */
    private String victimHolder;

    /** 解决方式: KILL_VICTIM/ROLLBACK/WAIT */
    private String resolveAction;

    /** 解决结果: SUCCESS/FAILED */
    private String resolveResult;

    private LocalDateTime resolveTime;

    /** 详细信息 */
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
