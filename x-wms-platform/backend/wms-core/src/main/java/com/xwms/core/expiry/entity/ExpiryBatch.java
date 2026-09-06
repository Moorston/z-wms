package com.xwms.core.expiry.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 效期批次 */
@Data
@TableName("wms_expiry_batch")
public class ExpiryBatch {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 批次号 */
    private String batchNo;

    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 过期日期 */
    private LocalDate expiryDate;

    /** 保质期(天) */
    private Integer shelfLifeDays;

    /** 剩余天数 */
    private Integer remainDays;

    /** 批次总数量 */
    private BigDecimal totalQty;

    /** 可用数量 */
    private BigDecimal availableQty;

    /** 预警级别: NORMAL/W1/W2/W3/EXPIRED */
    private String warningLevel;

    /** 效期状态: NORMAL/NEAR_EXPIRY/EXPIRED */
    private String expiryStatus;

    /** 入库单号 */
    private String inboundNo;

    /** 供应商 */
    private String supplierCode;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
