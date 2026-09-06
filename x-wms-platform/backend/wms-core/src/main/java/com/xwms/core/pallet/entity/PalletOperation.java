package com.xwms.core.pallet.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 托盘操作记录表 记录托盘的所有操作：码盘、拆盘、组盘、移动、封存、解封等 */
@Data
@TableName("wms_pallet_operation")
public class PalletOperation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作记录号 */
    private String operationNo;

    /** LPN号/托盘号 */
    private String lpnNo;

    /**
     * 操作类型：PALLETIZE码盘/DEPALLETIZE拆盘/MERGE合并/SPLIT拆分/MOVE移动/SEAL封存/UNSEAL解封/DAMAGE损坏/REPAIR修复/SCRAP报废
     */
    private String operationType;

    /** 操作前状态 */
    private String beforeStatus;

    /** 操作后状态 */
    private String afterStatus;

    /** 操作前库位 */
    private String beforeLocation;

    /** 操作后库位 */
    private String afterLocation;

    /** 操作前数量 */
    private BigDecimal beforeQty;

    /** 操作后数量 */
    private BigDecimal afterQty;

    /** 操作数量（变化量） */
    private BigDecimal operationQty;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联出库单号 */
    private String outboundNo;

    /** 关联波次号 */
    private String waveNo;

    /** 商品编码（单品操作时） */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 批次号 */
    private String batchNo;

    /** 序列号 */
    private String serialNo;

    /** 源托盘号（合并/拆分时） */
    private String sourceLpn;

    /** 目标托盘号（合并/拆分时） */
    private String targetLpn;

    /** 操作原因 */
    private String reason;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private LocalDateTime operationTime;

    /** 设备号（PDA/RF） */
    private String deviceNo;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
