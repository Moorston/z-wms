package com.xwms.core.pallet.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 托盘明细表 记录托盘上的商品明细 */
@Data
@TableName("wms_pallet_detail")
public class PalletDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 明细号 */
    private String detailNo;

    /** LPN号/托盘号 */
    private String lpnNo;

    /** 行号 */
    private Integer lineNo;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联入库明细号 */
    private String inboundDetailNo;

    /** 关联收货任务明细号 */
    private String receiptDetailNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 商品条码 */
    private String barcode;

    /** 规格型号 */
    private String spec;

    /** 单位 */
    private String unit;

    /** 包装代码 */
    private String packageCode;

    /** 包装数量 */
    private BigDecimal packageQty;

    /** 数量 */
    private BigDecimal qty;

    /** 已上架数量 */
    private BigDecimal putawayQty;

    /** 已出库数量 */
    private BigDecimal shippedQty;

    /** 剩余数量 */
    private BigDecimal remainingQty;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

    /** 序列号（单个商品时） */
    private String serialNo;

    /** 商品重量（kg） */
    private BigDecimal productWeight;

    /** 商品体积（m³） */
    private BigDecimal productVolume;

    /** 商品高度（cm） */
    private BigDecimal productHeight;

    /** 状态：ON_PALLET在托盘上/PUTAWAYED已上架/SHIPPED已出库/REMOVED已移除 */
    private String status;

    /** 码盘人 */
    private String palletizedBy;

    /** 码盘时间 */
    private LocalDateTime palletizedTime;

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
