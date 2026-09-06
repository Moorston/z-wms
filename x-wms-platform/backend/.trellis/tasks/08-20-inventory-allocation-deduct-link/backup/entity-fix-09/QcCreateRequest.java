package com.xwms.core.qc.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建质检单请求
 */
@Data
public class QcCreateRequest {
    private String refType;       // INBOUND/RETURN/INSTOCK/OUTBOUND
    private String refNo;
    private Long refItemId;
    private String sku;
    private String barcode;
    private String productName;
    private String supplierCode;
    private String ownerCode;
    private String warehouseCode;
    private String locationCode;
    private String batchNo;
    private BigDecimal lotQty;
}
