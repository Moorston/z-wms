package com.xwms.core.combo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 组合拆解单 */
@Data
@TableName("wms_combo_disassemble")
public class ComboDisassemble {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 拆解单号 */
    private String disassembleNo;

    /** 组合品编码 */
    private String comboCode;

    /** 组合品名称 */
    private String comboName;

    private String warehouseCode;
    private String ownerCode;

    /** 拆解数量 */
    private BigDecimal disassembleQty;

    /** 状态: PENDING/PROCESSING/COMPLETED/CANCELLED */
    private String status;

    /** 来源: MANUAL/AUTO/ORDER */
    private String sourceType;

    /** 来源单号 */
    private String sourceNo;

    private String operator;
    private LocalDateTime disassembleTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
