package com.xwms.core.sortingreceipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 整理收货明细表 记录每个SKU（独色独码）的整理数量 */
@Data
@TableName("wms_sorting_receipt_detail")
public class SortingReceiptDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 整理收货单号 */
    private String receiptNo;

    /** 行号 */
    private Integer lineNo;

    /** 商品编码（独色独码SKU） */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 款式编码 */
    private String styleCode;

    /** 颜色编码 */
    private String colorCode;

    /** 尺码 */
    private String sizeCode;

    /** 配比数量（一个配比箱中该SKU的数量） */
    private BigDecimal ratioQty;

    /** 预期数量（配比箱数 * 配比数量） */
    private BigDecimal expectedQty;

    /** 已扫描数量 */
    private BigDecimal scannedQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 状态：PENDING待扫描/SCANNING扫描中/COMPLETED已完成 */
    private String status;

    /** 是否满箱：Y/N */
    private String isFullBox;

    /** 满箱数量（包装箱数量） */
    private BigDecimal fullBoxQty;

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
