package com.xwms.core.lock.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 锁等待队列 */
@Data
@TableName("wms_lock_wait_queue")
public class LockWaitQueue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String waitId;

    /** 等待的锁键 */
    private String lockKey;

    /** 请求锁类型 */
    private String lockType;

    /** 请求者 */
    private String requester;

    private String requesterIp;
    private String businessNo;

    private LocalDateTime waitStartTime;
    private LocalDateTime waitTimeout;

    /** 状态: WAITING/ACQUIRED/TIMEOUT/CANCELLED */
    private String status;

    private LocalDateTime acquireTime;

    /** 优先级 */
    private Integer priority;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
