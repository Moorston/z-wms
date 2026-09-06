package com.xwms.core.outbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 发运记录 */
@Data
@TableName("wms_ship_record")
public class ShipRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordNo;
    private String outboundNo;
    private String carrier;
    private String trackingNo;

    /** 发运数量 */
    private BigDecimal shipQty;

    /** 包裹数 */
    private Integer packageCount;

    /** 重量 */
    private BigDecimal weight;

    /** 体积 */
    private BigDecimal volume;

    /** 发运状态: CREATED/SHIPPED/DELIVERED/FAILED */
    private String shipStatus;

    private String operator;
    private LocalDateTime shipTime;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
