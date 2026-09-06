package com.xwms.core.wave.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 波次明细（波次关联的出库单） */
@Data
@TableName("wms_wave_detail")
public class WaveDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String waveNo;
    private String outboundNo;
    private String customerCode;
    private String carrier;
    private Integer priority;

    /** 状态: PENDING/PICKING/PICKED/PACKED/SHIPPED */
    private String status;

    private BigDecimal pickedQty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
