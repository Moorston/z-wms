package com.xwms.core.performance.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 作业记录 */
@Data
@TableName("wms_work_record")
public class WorkRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordNo;
    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String warehouseCode;

    /** 作业类型: RECEIVING/PUTAWAY/PICKING/CHECKING/PACKING/COUNTING/MOVING/VAS/REPLENISH */
    private String workType;

    private String taskNo;
    private String sourceNo;
    private String sku;
    private String productName;
    private String batchNo;
    private String locationFrom;
    private String locationTo;

    /** 作业数量 */
    private BigDecimal quantity;

    /** 作业重量 */
    private BigDecimal weight;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /** 耗时(分钟) */
    private Integer durationMin;

    /** 差错数量 */
    private Integer errorCount;

    /** 质量评分(0-100) */
    private BigDecimal qualityScore;

    /** 使用设备 */
    private Long equipmentId;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
