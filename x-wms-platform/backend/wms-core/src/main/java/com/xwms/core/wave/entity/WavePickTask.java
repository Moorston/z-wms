package com.xwms.core.wave.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 波次拣货任务（按SKU+库位拆分） */
@Data
@TableName("wms_wave_pick_task")
public class WavePickTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String taskNo;
    private String waveNo;
    private String skuCode;
    private String batchNo;
    private String fromLocation;
    private BigDecimal pickQty;
    private BigDecimal pickedQty;
    private BigDecimal differenceQty;
    private String picker;

    /** 状态: PENDING/PICKING/DONE/EXCEPTION */
    private String status;

    /** 路径顺序 */
    private Integer pathOrder;

    private LocalDateTime pickTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
