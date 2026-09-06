package com.xwms.core.query.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存分析指标 */
@Data
@TableName("wms_inventory_metric")
public class InventoryMetric {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String metricId;
    private LocalDate metricDate;
    private String metricType;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String categoryCode;

    private BigDecimal metricValue;
    private BigDecimal metricValue2;
    private BigDecimal metricValue3;
    private String metricText;

    private Integer rankNo;
    private String levelCode;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
