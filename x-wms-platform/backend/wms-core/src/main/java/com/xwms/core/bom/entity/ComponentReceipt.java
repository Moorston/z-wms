package com.xwms.core.bom.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 组件扫描收货记录表 记录组件扫描收货过程，子件扫描满足BOM组合时组装为父件收货 */
@Data
@TableName("wms_component_receipt")
public class ComponentReceipt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 组件收货单号 */
    private String receiptNo;

    /** BOM编码 */
    private String bomCode;

    /** 父件商品编码 */
    private String parentSkuCode;

    /** 父件商品名称 */
    private String parentSkuName;

    /** ASN号 */
    private String asnNo;

    /** 入库单号 */
    private String inboundNo;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 批次号 */
    private String batchNo;

    /** 收货库位 */
    private String locationCode;

    /** 父件收货数量 */
    private BigDecimal parentQty;

    /** 已扫描子件种类数 */
    private Integer scannedChildCount;

    /** 需要子件种类数 */
    private Integer requiredChildCount;

    /** 状态：SCANNING扫描中/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 是否满足BOM组合：Y/N */
    private String bomMatched;

    /** 操作人 */
    private String operator;

    /** 扫描开始时间 */
    private LocalDateTime startTime;

    /** 扫描完成时间 */
    private LocalDateTime finishTime;

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
