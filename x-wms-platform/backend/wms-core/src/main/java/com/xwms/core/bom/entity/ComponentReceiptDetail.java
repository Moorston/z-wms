package com.xwms.core.bom.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 组件扫描收货明细表 记录每个子件的扫描数量 */
@Data
@TableName("wms_component_receipt_detail")
public class ComponentReceiptDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组件收货单号 */
    private String receiptNo;

    /** BOM编码 */
    private String bomCode;

    /** 子件商品编码 */
    private String childSkuCode;

    /** 子件商品名称 */
    private String childSkuName;

    /** BOM要求数量 */
    private BigDecimal requiredQty;

    /** 已扫描数量 */
    private BigDecimal scannedQty;

    /** 是否满足：Y/N */
    private String satisfied;

    /** 是否可选件：Y/N */
    private String isOptional;

    /** 扫描时间 */
    private LocalDateTime scanTime;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
