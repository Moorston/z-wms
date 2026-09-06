package com.xwms.core.recommend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 路径推荐 */
@Data
@TableName("wms_path_recommend")
public class PathRecommend {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recommendId;
    private String warehouseCode;
    private String ownerCode;
    private String waveId;
    private String pickerId;
    private String pickerName;

    /** 拣货模式: PICK_TO_CART/SOW_TO_BIN/RELAY */
    private String pickMode;

    private Integer locationCount;
    private Integer skuCount;
    private BigDecimal totalQuantity;

    /** 推荐路径(JSON数组, 库位序列) */
    private String recommendPath;

    private String startLocation;
    private String endLocation;

    /** 预计距离(米) */
    private BigDecimal estimateDistance;

    /** 预计时间(分钟) */
    private Long estimateTime;

    /** 路径算法: NEAREST_NEIGHBOR/TSP/GENETIC/SHORTEST_PATH */
    private String pathAlgorithm;

    /** 拥堵规避: Y/N */
    private String congestionAvoidance;

    /** 避免重走: Y/N */
    private String revisitAvoidance;

    /** 优先级: URGENT/HIGH/NORMAL/LOW */
    private String priority;

    /** 置信度(%) */
    private BigDecimal confidence;

    private String reason;

    /** 状态: PENDING/ACCEPTED/REJECTED/EXECUTED */
    private String status;

    private String operator;
    private LocalDateTime operateTime;

    /** 实际路径(JSON数组) */
    private String actualPath;

    /** 实际距离(米) */
    private BigDecimal actualDistance;

    /** 实际时间(分钟) */
    private Long actualTime;

    private LocalDateTime recommendTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
