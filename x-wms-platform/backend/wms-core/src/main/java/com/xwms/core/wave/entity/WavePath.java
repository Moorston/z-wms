package com.xwms.core.wave.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 波次路径（拣货路径规划） */
@Data
@TableName("wms_wave_path")
public class WavePath {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String waveNo;

    /** 路径顺序 */
    private Integer pathOrder;

    private String locationCode;

    /** 库位X坐标 */
    private BigDecimal locationX;

    /** 库位Y坐标 */
    private BigDecimal locationY;

    /** 库位Z坐标 */
    private BigDecimal locationZ;

    /** 到下一库位距离 */
    private BigDecimal distance;

    /** 该库位待拣SKU数 */
    private Integer skuCount;

    /** 该库位待拣总数量 */
    private BigDecimal totalQty;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
