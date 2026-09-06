package com.xwms.core.replenish.dto;

import java.math.BigDecimal;

import lombok.Data;

/** 创建补货任务请求 */
@Data
public class ReplenishCreateRequest {
    private Long ruleId;
    private String sku;
    private String barcode;
    private String productName;
    private String batchNo;
    private String ownerCode;
    private String warehouseCode;
    private String fromLocation;
    private String toLocation;
    private BigDecimal planQty;
    private String replenishType; // NORMAL/URGENT/CROSSDOCK/PRESALE/PERIODIC
    private Integer priority;
    private String triggerSource; // AUTO/MANUAL/SHORTAGE/SCHEDULE/PRESALE
}
