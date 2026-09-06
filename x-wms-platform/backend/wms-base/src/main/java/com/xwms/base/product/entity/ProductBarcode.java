package com.xwms.base.product.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 产品条码 */
@Data
@TableName("wms_product_barcode")
public class ProductBarcode {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String barcode;
    private String skuCode;
    private String packageCode;

    /** 条码类型: EAN13/UPC/CODE128/QR/INTERNAL */
    private String barcodeType;

    /** 条码对应数量 */
    private BigDecimal quantity;

    /** 是否主条码 */
    private Integer isPrimary;

    private String status;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
