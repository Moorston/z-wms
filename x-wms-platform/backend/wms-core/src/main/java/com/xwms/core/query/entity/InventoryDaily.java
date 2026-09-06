package com.xwms.core.query.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存日报 */
@Data
@TableName("wms_inventory_daily")
public class InventoryDaily {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private LocalDate reportDate;
    private String warehouseCode;
    private String ownerCode;
    private String skuCode;
    private String skuName;

    private BigDecimal beginQty;
    private BigDecimal inboundQty;
    private BigDecimal outboundQty;
    private BigDecimal adjustInQty;
    private BigDecimal adjustOutQty;
    private BigDecimal endQty;

    private BigDecimal beginCost;
    private BigDecimal inboundCost;
    private BigDecimal outboundCost;
    private BigDecimal endCost;
    private BigDecimal avgCost;

    private BigDecimal turnoverDays;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
