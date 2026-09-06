package com.xwms.analytics.billing.dto;

import java.math.BigDecimal;

import lombok.Data;

/** 费用入账请求 */
@Data
public class BillingCreateRequest {
    private String feeType;
    private String ownerCode;
    private String customerCode;
    private String warehouseCode;
    private String refType;
    private String refNo;
    private BigDecimal quantity;
    private BigDecimal weight;
    private BigDecimal volume;
    private Integer days;
    private String sku;
    private String productName;
}
