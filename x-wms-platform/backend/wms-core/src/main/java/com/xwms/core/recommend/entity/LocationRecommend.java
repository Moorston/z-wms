package com.xwms.core.recommend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库位推荐 */
@Data
@TableName("wms_location_recommend")
public class LocationRecommend {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recommendId;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;
    private String batchNo;
    private BigDecimal quantity;

    /** 业务类型: INBOUND/MOVE/REPLENISH/RETURN */
    private String bizType;

    private String bizNo;

    /** 推荐库位列表(JSON数组) */
    private String recommendLocations;

    private String bestLocation;
    private BigDecimal bestScore;
    private String recommendReason;

    /** 考虑因素(JSON) */
    private String factors;

    /** 状态: PENDING/ACCEPTED/REJECTED/EXECUTED */
    private String status;

    private String operator;
    private LocalDateTime operateTime;
    private String selectedLocation;
    private LocalDateTime recommendTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
