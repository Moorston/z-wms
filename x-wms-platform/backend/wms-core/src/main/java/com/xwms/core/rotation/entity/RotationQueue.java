package com.xwms.core.rotation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 周转批次队列 */
@Data
@TableName("wms_rotation_queue")
public class RotationQueue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 队列ID(SKU+库位+货主) */
    private String queueId;

    private String skuCode;
    private String batchNo;
    private String locationCode;
    private String ownerCode;

    /** 可用数量 */
    private BigDecimal quantity;

    private LocalDateTime productionDate;
    private LocalDateTime expireDate;
    private LocalDateTime receiveDate;

    /** 排序值(根据周转规则生成) */
    private String sortValue;

    /** 排序序号 */
    private Integer sortOrder;

    /** 状态: ACTIVE/EXPIRED/FROZEN */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
