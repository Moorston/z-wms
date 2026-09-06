package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 退货单主表 支持客户退货、供应商退货、调拨退货等多种退货场景 */
@Data
@TableName("wms_return_order")
public class ReturnOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退货单号 */
    private String returnNo;

    /** 退货类型：CUSTOMER客户退货/SUPPLIER供应商退货/TRANSFER调拨退货/INTERNAL内部退货 */
    private String returnType;

    /** 原出库单号 */
    private String originalOutboundNo;

    /** 原入库单号（供应商退货时） */
    private String originalInboundNo;

    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 退货原因 */
    private String returnReason;

    /** 退货原因代码 */
    private String reasonCode;

    /** 退货总数量 */
    private BigDecimal totalQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 已上架数量 */
    private BigDecimal putawayQty;

    /** 合格数量 */
    private BigDecimal qualifiedQty;

    /** 不合格数量 */
    private BigDecimal unqualifiedQty;

    /**
     * 状态：CREATED已创建/RECEIVING收货中/RECEIVED已收货/QC质检中/QC_DONE质检完成/PUTAWAYING上架中/COMPLETED已完成/CANCELLED已取消
     */
    private String status;

    /** 是否需要质检：Y/N */
    private String needQc;

    /** 质检状态：NOT_NEEDED不需要/PENDING待检/INSPECTING质检中/PASSED合格/FAILED不合格 */
    private String qcStatus;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联ASN编组号 */
    private String groupNo;

    /** 退货申请时间 */
    private LocalDateTime applyTime;

    /** 预计到货时间 */
    private LocalDateTime expectedArrivalTime;

    /** 实际到货时间 */
    private LocalDateTime actualArrivalTime;

    /** 收货完成时间 */
    private LocalDateTime receiveCompleteTime;

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
