package com.xwms.base.batch.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 批次追踪日志 */
@Data
@TableName("wms_batch_trace_log")
public class BatchTraceLog {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String traceNo;

    /** 批号 */
    private String batchNo;

    private String skuCode;

    /** 操作类型: INBOUND/OUTBOUND/TRANSFER/ADJUST/QC/SPLIT/MERGE */
    private String operationType;

    /** 操作单号 */
    private String operationNo;

    /** 源库位 */
    private String fromLocation;

    /** 目标库位 */
    private String toLocation;

    /** 数量 */
    private BigDecimal quantity;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operationTime;

    /** 追踪数据(JSON) */
    private String traceData;

    /** 链路追踪ID */
    private String traceId;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
