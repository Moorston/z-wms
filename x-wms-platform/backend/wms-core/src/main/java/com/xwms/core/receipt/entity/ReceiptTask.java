package com.xwms.core.receipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 收货任务表 从ASN/入库单生成的收货任务，支持多种收货方式
 * 状态流转：PENDING待收货→RECEIVING收货中→PARTIAL部分收货→COMPLETED收货完成→CANCELLED已取消
 */
@Data
@TableName("wms_receipt_task")
public class ReceiptTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 收货任务号 */
    private String taskNo;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联PO号（可选） */
    private String poNo;

    /**
     * 收货方式：ASN整单/PARTIAL部分/PALLET码盘/SCAN扫描/BOX按箱/QUICK快捷/VISUAL可视化/MIX混单/COMPONENT组件/SORT整理/PRE预收货/BLIND盲收
     */
    private String receiptType;

    /** 扫描模式：BATCH批量/PIECE逐件/BOX逐箱/SERIAL序列号 */
    private String scanMode;

    /** 供应商编码 */
    private String supplierCode;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 收货库区 */
    private String receiveArea;

    /** 收货库位 */
    private String receiveLocation;

    /** 月台号 */
    private String dockNo;

    /** 预期数量 */
    private BigDecimal expectedQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 差异数量 */
    private BigDecimal differenceQty;

    /** 状态：PENDING/RECEIVING/PARTIAL/COMPLETED/CANCELLED */
    private String status;

    /** 是否需要质检：Y/N */
    private String qcRequired;

    /** 质检状态：NOT_REQUIRED/PENDING/PASSED/FAILED */
    private String qcStatus;

    /** 是否直接上架（收货后直接到存储库位）：Y/N */
    private String directPutaway;

    /** 车牌号 */
    private String vehicleNo;

    /** 司机姓名 */
    private String driverName;

    /** 司机电话 */
    private String driverPhone;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime completeTime;

    /** 收货人 */
    private String receiver;

    /** 备注 */
    private String remark;

    /** 来源：ASN/MANUAL/BLIND */
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
