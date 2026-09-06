package com.xwms.core.returnorder.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/** 退货收货请求 */
@Data
public class ReturnReceiveRequest {
    private Long returnId;
    private String receiver;
    private List<ReceiveItem> items;

    @Data
    public static class ReceiveItem {
        private Long itemId;
        private BigDecimal receivedQty;
    }
}
