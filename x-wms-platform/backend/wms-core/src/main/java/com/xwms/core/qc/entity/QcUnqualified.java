package com.xwms.core.qc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 不合格品 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_qc_unqualified")
public class QcUnqualified extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 质检单ID */
    private Long qcId;

    /** SKU编码 */
    private String sku;

    /** 条码 */
    private String barcode;

    /** 批次号 */
    private String batchNo;

    /** 数量 */
    private BigDecimal qty;

    /** 不合格类型 */
    private String unqualifiedType;

    /** 缺陷描述 */
    private String defectDesc;

    /** 处理方式: RETURN_SUPPLIER/DESTROY/REWORK/DOWNGRADE/PENDING */
    private String handleMethod;

    /** 处理数量 */
    private BigDecimal handleQty;

    /** 处理状态: PENDING/PROCESSING/DISPOSED */
    private String handleStatus;

    /** 不合格品区库位 */
    private String locationCode;

    /** 处理人 */
    private String handler;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 销毁证明 */
    private String disposeCert;

    /** 备注 */
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
