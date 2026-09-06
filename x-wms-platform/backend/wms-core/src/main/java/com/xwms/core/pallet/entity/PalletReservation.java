package com.xwms.core.pallet.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 码盘预约表 收货前预计算码盘方案和上架库位，提高收货效率 状态流转：PENDING待预约→RESERVED已预约→IN_PROGRESS进行中→COMPLETED已完成→CANCELLED已取消
 */
@Data
@TableName("wms_pallet_reservation")
public class PalletReservation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预约号 */
    private String reservationNo;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联PO号 */
    private String poNo;

    /** 供应商编码 */
    private String supplierCode;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 预约托盘数 */
    private Integer reservedPalletCount;

    /** 实际托盘数 */
    private Integer actualPalletCount;

    /** 预约总数量 */
    private BigDecimal reservedTotalQty;

    /** 实际总数量 */
    private BigDecimal actualTotalQty;

    /** 预约收货库区 */
    private String reservedReceiveArea;

    /** 预约收货库位 */
    private String reservedReceiveLocation;

    /** 预约月台号 */
    private String reservedDockNo;

    /** 预约上架库区 */
    private String reservedPutawayArea;

    /** 预约上架库位（多个库位用逗号分隔） */
    private String reservedPutawayLocations;

    /** 码盘策略：MIX_SKU混SKU/SINGLE_SKU单SKU/MIX_BATCH混批次/SINGLE_BATCH单批次 */
    private String palletizeStrategy;

    /** 上架策略：NEAREST/FIFO/FEFO/ZONE/HEIGHT/WEIGHT */
    private String putawayStrategy;

    /** 状态：PENDING/RESERVED/IN_PROGRESS/COMPLETED/CANCELLED */
    private String status;

    /** 预约时间 */
    private LocalDateTime reservationTime;

    /** 预约人 */
    private String reservedBy;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 操作人 */
    private String operator;

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
