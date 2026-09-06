package com.xwms.core.vas.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

/** 创建VAS工单请求 */
@Data
public class VasOrderCreateRequest {
    private String orderType; // INBOUND/OUTBOUND/STOCK/STANDALONE
    private String sourceOrderNo;
    private String sourceOrderType;
    private String customerCode;
    private String ownerCode;
    private String warehouseCode;
    private String serviceCode;
    private BigDecimal planQty;
    private Integer priority;
    private String locationCode;
    private String batchNo;
    private LocalDateTime planStartTime;
    private LocalDateTime planFinishTime;
    private List<VasItemRequest> items;

    @Data
    public static class VasItemRequest {
        private String sku;
        private String barcode;
        private String productName;
        private String batchNo;
        private String fromLocation;
        private String toLocation;
        private BigDecimal planQty;
        private String beforeSpec;
    }
}
