package com.xwms.core.storereceipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 门店收货记录表 仓库发货后，门店收货人做收货清点，适用于无ERP/POS系统环境 */
@Data
@TableName("wms_store_receipt")
public class StoreReceipt {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String receiptNo;
    private String storeCode;
    private String storeName;
    private String outboundNo;
    private String waveNo;
    private String carrierCode;
    private String trackingNo;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    private BigDecimal differenceQty;
    private String status; // PENDING待收货/RECEIVED已收货/EXCEPTION异常
    private String receiver;
    private LocalDateTime receiveTime;
    private String remark;
    private String createdBy;
    private LocalDateTime createdTime;
    private String updatedBy;
    private LocalDateTime updatedTime;
}
