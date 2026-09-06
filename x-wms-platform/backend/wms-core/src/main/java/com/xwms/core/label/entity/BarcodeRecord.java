package com.xwms.core.label.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 条码记录 */
@Data
@TableName("wms_barcode_record")
public class BarcodeRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordId;

    /** 条码 */
    private String barcode;

    /** 条码类型: SKU/LOCATION/BATCH/CONTAINER/ORDER/PALLET/SERIAL */
    private String barcodeType;

    private String ruleCode;
    private String warehouseCode;
    private String ownerCode;

    /** 业务主键(SKU/库位/批次等) */
    private String bizKey;

    private String bizType;

    /** 业务类型 */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    /** 商品编码 */
    private String sku;

    /** 批号 */
    private String batchNo;

    /** 数量 */
    private java.math.BigDecimal quantity;

    /** 生成人 */
    private String generatedBy;

    /** 序列号 */
    private Long sequenceNo;

    private LocalDateTime generateTime;

    /** 打印次数 */
    private Integer printCount;

    private LocalDateTime lastPrintTime;

    /** 扫描次数 */
    private Integer scanCount;

    private LocalDateTime lastScanTime;

    /** 状态: ACTIVE/USED/EXPIRED/DISABLED */
    private String status;

    private LocalDateTime expireTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
