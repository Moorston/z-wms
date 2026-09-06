package com.xwms.core.inbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** ASN(到货通知)单 */
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

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联采购单号 */
    private String poNo;

    private String supplierCode;

    /** 货主编码(业务字段) */
    private String ownerCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String warehouseCode;

    /** 收货库区 */
    private String receiveArea;

    /** 收货库位 */
    private String receiveLocation;

    /** 预计到货日期 */
    private LocalDateTime expectedDate;

    /** 实际到货日期 */
    private LocalDateTime actualDate;

    /** 预期到货数量 */
    private BigDecimal expectedQty;

    private BigDecimal totalQty;
    private BigDecimal receivedQty;

    /** 是否需要质检: Y/N */
    private String qcRequired;

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
