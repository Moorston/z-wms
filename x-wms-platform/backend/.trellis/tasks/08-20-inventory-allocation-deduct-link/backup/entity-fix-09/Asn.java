package com.xwms.core.inbound.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ASN(到货通知)单
 */
@Data
@TableName("wms_asn")
public class Asn {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String asnNo;

    /** ASN类型: PURCHASE/RETURN/TRANSFER/PRODUCTION */
    private String asnType;

    /** 来源单号 */
    private String refNo;

    private String supplierCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String warehouseCode;

    /** 预计到货日期 */
    private LocalDateTime expectedDate;

    /** 实际到货日期 */
    private LocalDateTime actualDate;

    private BigDecimal totalQty;
    private BigDecimal receivedQty;

    /** 状态: CREATED/SHIPPED/RECEIVING/RECEIVED/CANCELLED */
    private String status;

    private String carrier;
    private String trackingNo;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
