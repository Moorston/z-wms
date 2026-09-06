package com.xwms.core.qc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 让步接收 */
@Data
@TableName("wms_qc_concession")
public class QcConcession {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 质检单ID */
    private Long qcId;

    /** SKU编码 */
    private String sku;

    /** 批次号 */
    private String batchNo;

    /** 数量 */
    private BigDecimal qty;

    /** 让步原因 */
    private String reason;

    /** 缺陷描述 */
    private String defectDesc;

    /** 影响程度: LOW/MEDIUM/HIGH */
    private String impactLevel;

    /** 申请人 */
    private String applicant;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审批人 */
    private String approver;

    /** 审批时间 */
    private LocalDateTime approveTime;

    /** 审批意见 */
    private String approveOpinion;

    /** 状态: PENDING/APPROVED/REJECTED */
    private String status;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
