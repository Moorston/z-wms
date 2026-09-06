package com.xwms.core.storereceipt.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.xwms.core.storereceipt.entity.StoreReceipt;
import com.xwms.core.storereceipt.mapper.StoreReceiptMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 门店收货服务 仓库发货后，门店收货人做收货清点，适用于无ERP/POS系统环境 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StoreReceiptService {

    private final StoreReceiptMapper storeReceiptMapper;

    /** 门店收货确认 输入收货人（必输）、订单号或箱号，系统查询已发货订单装箱明细，收货人确认 */
    @Transactional(rollbackFor = Exception.class)
    public StoreReceipt confirmStoreReceipt(
            String storeCode,
            String storeName,
            String outboundNo,
            BigDecimal expectedQty,
            BigDecimal receivedQty,
            String receiver,
            String remark) {
        log.info("门店收货确认: store={}, outbound={}, 实收={}", storeCode, outboundNo, receivedQty);

        StoreReceipt receipt = new StoreReceipt();
        receipt.setReceiptNo("SR" + System.currentTimeMillis());
        receipt.setStoreCode(storeCode);
        receipt.setStoreName(storeName);
        receipt.setOutboundNo(outboundNo);
        receipt.setExpectedQty(expectedQty);
        receipt.setReceivedQty(receivedQty);
        receipt.setDifferenceQty(expectedQty.subtract(receivedQty));
        receipt.setStatus(receivedQty.compareTo(expectedQty) == 0 ? "RECEIVED" : "EXCEPTION");
        receipt.setReceiver(receiver);
        receipt.setReceiveTime(LocalDateTime.now());
        receipt.setRemark(remark);
        receipt.setCreatedBy(receiver);
        receipt.setCreatedTime(LocalDateTime.now());
        storeReceiptMapper.insert(receipt);

        log.info("门店收货确认完成: receiptNo={}, 状态={}", receipt.getReceiptNo(), receipt.getStatus());
        return receipt;
    }

    /** 查询门店收货记录 */
    public List<StoreReceipt> getStoreReceipts(String storeCode) {
        return storeReceiptMapper.selectList(
                new LambdaQueryWrapper<StoreReceipt>()
                        .eq(storeCode != null, StoreReceipt::getStoreCode, storeCode)
                        .orderByDesc(StoreReceipt::getCreatedTime));
    }

    /** 根据出库单查询门店收货 */
    public StoreReceipt getByOutboundNo(String outboundNo) {
        List<StoreReceipt> list = storeReceiptMapper.selectByOutboundNo(outboundNo);
        return (list != null && !list.isEmpty()) ? list.get(0) : null;
    }
}
