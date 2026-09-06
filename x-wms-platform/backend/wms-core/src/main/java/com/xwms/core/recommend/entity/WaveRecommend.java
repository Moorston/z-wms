package com.xwms.core.recommend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 波次推荐 */
@Data
@TableName("wms_wave_recommend")
public class WaveRecommend {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recommendId;
    private String warehouseCode;
    private String ownerCode;

    /** 波次类型: NORMAL/URGENT/BULK/APPOINTMENT */
    private String waveType;

    /** 推荐策略: TIME/QUANTITY/CARRIER/AREA/PRIORITY */
    private String recommendStrategy;

    private Integer orderCount;
    private Integer skuCount;
    private BigDecimal totalQuantity;

    /** 推荐订单列表(JSON数组) */
    private String recommendOrders;

    private String recommendWaveName;

    /** 预计拣货时间(分钟) */
    private Long estimatePickTime;

    /** 预计拣货人数 */
    private Integer estimatePickers;

    /** 优先级: URGENT/HIGH/NORMAL/LOW */
    private String priority;

    /** 置信度(%) */
    private BigDecimal confidence;

    private String reason;

    /** 状态: PENDING/ACCEPTED/REJECTED/EXECUTED */
    private String status;

    private String operator;
    private LocalDateTime operateTime;
    private String relatedWaveId;
    private LocalDateTime recommendTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
