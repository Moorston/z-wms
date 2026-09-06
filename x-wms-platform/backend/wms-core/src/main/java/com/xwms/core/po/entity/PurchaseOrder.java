package com.xwms.core.po.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 采购订单（PO）主表 作为ASN的来源依据，提供预到货参考
 * 状态流转：CREATED创建→RELEASED已释放→PARTIAL_RECEIVED部分收货→FULLY_RECEIVED完全收货→COMPLETED订单完成→CANCELLED已取消
 */
@Data
@TableName("wms_purchase_order")
public class PurchaseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 采购订单号 */
    private String poNo;

    /** 外部订单号（ERP订单号） */
    private String externalPoNo;

    /** 订单类型：STANDARD标准采购/RETURN退货采购/CONSIGNMENT寄售/VMI供应商管理库存 */
    private String poType;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 采购日期 */
    private LocalDate poDate;

    /** 预期到货日期 */
    private LocalDate expectedArrivalDate;

    /** 预期到货开始时间（RCV_TIM_CTL参数控制） */
    private LocalDateTime expectedStartTime;

    /** 预期到货结束时间 */
    private LocalDateTime expectedEndTime;

    /** 订单总数量 */
    private BigDecimal totalQty;

    /** 已释放数量（提取到ASN的数量） */
    private BigDecimal releasedQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 已入库数量 */
    private BigDecimal putawayQty;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 币种 */
    private String currency;

    /** 状态：CREATED/RELEASED/PARTIAL_RECEIVED/FULLY_RECEIVED/COMPLETED/CANCELLED */
    private String status;

    /** 释放状态：UNRELEASED未释放/PARTIAL部分释放/FULLY完全释放 */
    private String releaseStatus;

    /** 是否与ASN关联（ASN收货后自动更新PO收货数量） */
    private String asnLinked;

    /** 采购合同号 */
    private String contractNo;

    /** 采购员 */
    private String buyer;

    /** 审批状态：DRAFT草稿/PENDING审批中/APPROVED已审批/REJECTED已拒绝 */
    private String approvalStatus;

    /** 审批人 */
    private String approver;

    /** 审批时间 */
    private LocalDateTime approvalTime;

    /** 备注 */
    private String remark;

    /** 来源：ERP接口/MANUAL手工/EXCEL导入 */
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
