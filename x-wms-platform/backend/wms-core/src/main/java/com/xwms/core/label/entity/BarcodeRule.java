package com.xwms.core.label.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 条码规则 */
@Data
@TableName("wms_barcode_rule")
public class BarcodeRule {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;

    /** 条码类型: SKU/LOCATION/BATCH/CONTAINER/ORDER/PALLET/SERIAL */
    private String barcodeType;

    private String warehouseCode;
    private String ownerCode;

    private String prefix;
    private String suffix;

    /** 序列号长度 */
    private Integer sequenceLength;

    /** 序列号起始值 */
    private Long sequenceStart;

    /** 当前序列号 */
    private Long sequenceCurrent;

    /** 日期格式: yyyyMMdd/yyMMdd */
    private String dateFormat;

    /** 是否包含日期: Y/N */
    private String includeDate;

    /** 是否包含货主: Y/N */
    private String includeOwner;

    /** 是否包含仓库: Y/N */
    private String includeWarehouse;

    /** 校验码类型: MOD10/MOD11/NONE */
    private String checksumType;

    /** 条码格式: CODE128/CODE39/QRCODE/EAN13 */
    private String barcodeFormat;

    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
