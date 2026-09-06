package com.xwms.core.combo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 组合组装单 */
@Data
@TableName("wms_combo_assemble")
public class ComboAssemble {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 组装单号 */
    private String assembleNo;

    /** 组合品编码 */
    private String comboCode;

    /** 组合品名称 */
    private String comboName;

    private String warehouseCode;
    private String ownerCode;

    /** 组装数量 */
    private BigDecimal assembleQty;

    /** 状态: PENDING/PROCESSING/COMPLETED/CANCELLED */
    private String status;

    /** 来源: MANUAL/AUTO/ORDER */
    private String sourceType;

    /** 来源单号 */
    private String sourceNo;

    private String operator;
    private LocalDateTime assembleTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
