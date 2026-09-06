package com.xwms.core.sortingreceipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 整理收货箱表 记录整理后生成的单一SKU箱信息 */
@Data
@TableName("wms_sorting_receipt_box")
public class SortingReceiptBox {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 箱号（整理后生成的单一SKU箱号） */
    private String boxNo;

    /** 整理收货单号 */
    private String receiptNo;

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

    /** 箱内数量 */
    private BigDecimal boxQty;

    /** 满箱数量（包装箱标准数量） */
    private BigDecimal fullBoxQty;

    /** 是否满箱：Y/N */
    private String isFull;

    /** 库位编码 */
    private String locationCode;

    /** 托盘号/LPN */
    private String lpnNo;

    /** 状态：CREATED已创建/RECEIVED已收货/PUTAWAYED已上架 */
    private String status;

    /** 操作人 */
    private String operator;

    /** 创建时间 */
    private LocalDateTime createdTime;

    /** 收货时间 */
    private LocalDateTime receivedTime;

    /** 备注 */
    private String remark;

    /** 创建人 */
    private String createdBy;

    /** 更新人 */
    private String updatedBy;

    /** 更新时间 */
    private LocalDateTime updatedTime;
}
