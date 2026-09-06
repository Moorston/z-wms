package com.xwms.core.stocktake.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/** 创建盘点任务请求 */
@Data
public class StocktakeCreateRequest {
    private String taskName;
    private String stocktakeType; // FULL/AREA/SKU/CYCLE/RANDOM
    private String warehouseCode;
    private String areaCode;
    private String locationFrom;
    private String locationTo;
    private String skuList; // JSON
    private String ownerCode;
    private String batchNo;
    private String abcClass;
    private LocalDateTime planStartTime;
    private LocalDateTime planEndTime;
    private Integer freezeFlag;
    private Integer blindCount;
    private BigDecimal recountThreshold;
    private Integer priority;
    private String assignee;
    private String checker;
}
