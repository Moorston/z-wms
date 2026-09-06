package com.xwms.core.sortingreceipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 整理收货单表 服装行业配比箱拆箱整理，用于配比箱（箱内混SKU，多为同款不同尺码，尺码比例固定）拆箱入库，整理为独色独码的单一SKU箱 */
@Data
@TableName("wms_sorting_receipt")
public class SortingReceipt {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整理收货单号 */
    private String receiptNo;

    /** ASN号 */
    private String asnNo;

    /** 入库单号 */
    private String inboundNo;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 配比箱组号（同款同色的一组配比箱） */
    private String groupNo;

    /** 款式编码 */
    private String styleCode;

    /** 颜色编码 */
    private String colorCode;

    /** 配比箱数量 */
    private Integer boxCount;

    /** 已拆箱数量 */
    private Integer unpackedBoxCount;

    /** 总数量 */
    private BigDecimal totalQty;

    /** 已整理数量 */
    private BigDecimal sortedQty;

    /** 状态：PENDING待整理/SORTING整理中/COMPLETED已完成/CANCELLED已取消 */
    private String status;

    /** 收货库位（整理后单一SKU箱的存放库位） */
    private String locationCode;

    /** 操作人 */
    private String operator;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
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
