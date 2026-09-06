package com.xwms.core.storereceipt.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.xwms.core.storereceipt.entity.StoreReceipt;
import com.xwms.core.storereceipt.service.StoreReceiptService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/store-receipt")
@RequiredArgsConstructor
public class StoreReceiptController {

    private final StoreReceiptService storeReceiptService;

    @PostMapping("/confirm")
    public StoreReceipt confirmStoreReceipt(
            @RequestParam String storeCode,
            @RequestParam String storeName,
            @RequestParam String outboundNo,
            @RequestParam BigDecimal expectedQty,
            @RequestParam BigDecimal receivedQty,
            @RequestParam String receiver,
            @RequestParam(required = false) String remark) {
        return storeReceiptService.confirmStoreReceipt(
                storeCode, storeName, outboundNo, expectedQty, receivedQty, receiver, remark);
    }

    @GetMapping("/store/{storeCode}")
    public List<StoreReceipt> getStoreReceipts(@PathVariable String storeCode) {
        return storeReceiptService.getStoreReceipts(storeCode);
    }

    @GetMapping("/outbound/{outboundNo}")
    public StoreReceipt getByOutboundNo(@PathVariable String outboundNo) {
        return storeReceiptService.getByOutboundNo(outboundNo);
    }
}
