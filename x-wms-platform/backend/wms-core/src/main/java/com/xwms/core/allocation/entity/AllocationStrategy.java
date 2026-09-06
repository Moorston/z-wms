package com.xwms.core.allocation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 分配策略配置 */
@Data
@TableName("wms_allocation_strategy")
public class AllocationStrategy {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联规则编码 */
    private String ruleCode;

    /** 策略类型: FIFO/FEFO/LIFO/LEFO/MANUAL */
    private String strategyType;

    /** 排序字段 */
    private String sortField;

    /** 排序方向: ASC/DESC */
    private String sortOrder;

    /** 库位优先级: GOLDEN/NORMAL/REMOTE */
    private String locationPriority;

    /** 批次优先级: EARLIEST/LATEST/NEAREST */
    private String batchPriority;

    /** 是否允许拆单: Y/N */
    private String allowSplit;

    /** 最小分配数量 */
    private BigDecimal minAllocQty;

    /** 最大分配数量 */
    private BigDecimal maxAllocQty;

    /** 预占保留时间(小时) */
    private Integer reserveHours;

    /** 超时自动释放: Y/N */
    private String autoRelease;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
