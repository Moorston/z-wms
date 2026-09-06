package com.xwms.core.rf.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** RF任务 */
@Data
@TableName("wms_rf_task")
public class RfTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;

    /**
     * 任务类型: RECEIVING/PUTAWAY/PICKING/CHECKING/PACKING/MOVING/COUNTING/REPLENISH/VAS/RETURN/SORTING
     */
    private String taskType;

    /** 优先级: LOW/NORMAL/HIGH/URGENT */
    private String priority;

    /** 状态: PENDING/ASSIGNED/IN_PROGRESS/PAUSED/COMPLETED/CANCELLED */
    private String status;

    private String warehouseCode;
    private String areaCode;

    /** 关联业务单号 */
    private String sourceNo;

    /** 关联业务类型 */
    private String sourceType;

    private String sku;
    private String productName;
    private String batchNo;
    private String locationFrom;
    private String locationTo;

    private BigDecimal quantity;
    private BigDecimal quantityDone;

    /** 分配给(RF用户ID) */
    private String assignedTo;

    private String assignedName;
    private String pdaDeviceId;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 耗时(分钟) */
    private Integer durationMin;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
