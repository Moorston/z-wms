package com.xwms.core.returnorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/** 创建退货单请求 */
@Data
public class ReturnCreateRequest {
    private String returnType; // CUSTOMER/SUPPLIER/TRANSFER/INTERNAL
    private String sourceOrderNo;
    private String sourceOrderType;
    private String customerCode;
    private String customerName;
    private String supplierCode;
    private String ownerCode;
    private String warehouseCode;
    private String returnReason;
    private String reasonCode; // QUALITY/WRONG/DAMAGE/EXPIRE/OTHER
    private Integer needQc;
    private Integer needRefund;
    private String trackingNo;
    private String carrierCode;
    private LocalDateTime expectArriveTime;
    private List<ReturnItemRequest> items;

    @Data
    public static class ReturnItemRequest {
        private Long sourceItemId;
        private String sku;
        private String barcode;
        private String productName;
        private String batchNo;
        private BigDecimal planQty;
        private BigDecimal unitPrice;
    }
}
