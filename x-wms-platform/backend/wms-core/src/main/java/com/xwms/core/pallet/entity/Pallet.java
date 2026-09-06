package com.xwms.core.pallet.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 托盘/LPN主表 LPN (License Plate Number) 跟踪号，用于标识一个托盘或容器
 * 状态流转：EMPTY空托盘→IN_USE使用中→FULL满托盘→IN_TRANSIT在途→STORED已存储→SHIPPED已出库→DAMAGED损坏
 */
@Data
@TableName("wms_pallet")
public class Pallet {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** LPN号/托盘号 */
    private String lpnNo;

    /** 托盘类型：STANDARD标准托盘/CHEP欧标托盘/SMALL小托盘/BIG大托盘/CARTON纸箱/BAG袋子 */
    private String palletType;

    /** 托盘状态：EMPTY/IN_USE/FULL/IN_TRANSIT/STORED/SHIPPED/DAMAGED */
    private String status;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 当前库区 */
    private String areaCode;

    /** 当前库位 */
    private String locationCode;

    /** 源库位（移动前） */
    private String sourceLocation;

    /** 目标库位（移动目标） */
    private String targetLocation;

    /** 关联ASN号（收货码盘时） */
    private String asnNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联收货任务号 */
    private String receiptTaskNo;

    /** 关联出库单号（出库码盘时） */
    private String outboundNo;

    /** 关联波次号 */
    private String waveNo;

    /** 托盘上SKU种类数 */
    private Integer skuCount;

    /** 托盘总数量 */
    private BigDecimal totalQty;

    /** 托盘总重量（kg） */
    private BigDecimal totalWeight;

    /** 托盘总体积（m³） */
    private BigDecimal totalVolume;

    /** 最大承重（kg） */
    private BigDecimal maxWeight;

    /** 最大体积（m³） */
    private BigDecimal maxVolume;

    /** 最大高度（cm） */
    private BigDecimal maxHeight;

    /** 是否混SKU：Y/N */
    private String mixedSku;

    /** 是否混批次：Y/N */
    private String mixedBatch;

    /** 是否封存：Y/N（封存库位不允许再放货） */
    private String sealed;

    /** 封存时间 */
    private LocalDateTime sealedTime;

    /** 封存人 */
    private String sealedBy;

    /** 码盘人 */
    private String palletizedBy;

    /** 码盘时间 */
    private LocalDateTime palletizedTime;

    /** 最后操作人 */
    private String lastOperator;

    /** 最后操作时间 */
    private LocalDateTime lastOperationTime;

    /** 备注 */
    private String remark;

    /** 来源：RECEIVE收货/MANUAL手工/OUTBOUND出库/TRANSFER调拨 */
    private String source;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
