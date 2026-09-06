package com.xwms.core.stocktake.dto;

import java.math.BigDecimal;

import lombok.Data;

/** 盘点录入请求 */
@Data
public class StocktakeCountRequest {
    private Long itemId;
    private BigDecimal countQty;
    private String counter;
    private String remark;
}
