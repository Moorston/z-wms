package com.xwms.core.wave.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 波次单 */
@Data
@TableName("wms_wave")
public class Wave {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String waveNo;

    /** 波次类型: NORMAL/URGENT/BULK/COLD */
    private String waveType;

    private String warehouseCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    /** 拣货模式: PICK_BY_ORDER/PICK_BY_SKU/PICK_BY_WAVE */
    private String pickMode;

    /** 波次策略编码 */
    private String strategyCode;

    private Integer orderCount;
    private Integer skuCount;
    private BigDecimal totalQty;
    private BigDecimal pickedQty;

    /** 状态: CREATED/ALLOCATED/PICKING/PICKED/PACKING/DONE/CANCELLED */
    private String status;

    /** 优先级 1-10 */
    private Integer priority;

    private String assignPicker;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
