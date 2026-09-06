package com.xwms.core.label.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 标签模板 */
@Data
@TableName("wms_label_template")
public class LabelTemplate {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String templateCode;
    private String templateName;

    /** 模板类型: SKU/LOCATION/BATCH/CONTAINER/ORDER/PALLET/SERIAL */
    private String templateType;

    private String warehouseCode;
    private String ownerCode;

    /** 标签宽度(mm) */
    private BigDecimal width;

    /** 标签高度(mm) */
    private BigDecimal height;

    /** 方向: PORTRAIT竖版/LANDSCAPE横版 */
    private String orientation;

    /** 模板内容(JSON/ZPL) */
    private String templateContent;

    /** 模板格式: ZPL/EPL/HTML/PDF */
    private String templateFormat;

    /** 条码类型: CODE128/CODE39/QRCODE/EAN13 */
    private String barcodeType;

    /** 条码位置: TOP_LEFT/TOP_RIGHT/CENTER/BOTTOM_LEFT/BOTTOM_RIGHT */
    private String barcodePosition;

    /** 默认打印份数 */
    private Integer printCount;

    /** 默认打印机 */
    private String defaultPrinter;

    private String status;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
