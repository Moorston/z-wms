package com.xwms.core.lock.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存锁记录 */
@Data
@TableName("wms_inventory_lock")
public class InventoryLock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String lockId;

    /** 锁键(如 SKU:SKU001:LOC:A01) */
    private String lockKey;

    /** 锁范围 */
    private String lockScope;

    /** 锁类型: SHARED/EXCLUSIVE */
    private String lockType;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String locationCode;
    private String batchNo;

    /** 锁定数量 */
    private BigDecimal lockQuantity;

    /** 业务类型: OUTBOUND/INBOUND/TRANSFER/ADJUST/REPLENISH */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    /** 持有者(线程/服务) */
    private String holder;

    /** 持有者IP */
    private String holderIp;

    /** 状态: HELD/WAITING/RELEASED/TIMEOUT/DEADLOCK */
    private String status;

    private LocalDateTime acquireTime;
    private LocalDateTime expireTime;
    private LocalDateTime releaseTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
