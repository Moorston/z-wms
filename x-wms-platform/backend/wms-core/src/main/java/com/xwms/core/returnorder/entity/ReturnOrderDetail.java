package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 退货单明细表 */
@Data
@TableName("wms_return_order_detail")
public class ReturnOrderDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退货单号 */
    private String returnNo;

    /** 明细行号 */
    private Integer lineNo;

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

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

    /** 退货数量 */
    private BigDecimal returnQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 已上架数量 */
    private BigDecimal putawayQty;

    /** 合格数量 */
    private BigDecimal qualifiedQty;

    /** 不合格数量 */
    private BigDecimal unqualifiedQty;

    /** 退货原因 */
    private String returnReason;

    /** 退货原因代码 */
    private String reasonCode;

    /** 原出库数量 */
    private BigDecimal originalOutboundQty;

    /** 原出库单价 */
    private BigDecimal originalPrice;

    /** 库位编码（收货库位） */
    private String locationCode;

    /** 状态：PENDING待收货/RECEIVED已收货/QC质检中/QC_DONE质检完成/PUTAWAYED已上架/CANCELLED已取消 */
    private String status;

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
