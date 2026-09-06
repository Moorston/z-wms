package com.xwms.core.qc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 质检任务表 关联收货任务/上架任务，控制收货/上架权限 质检时机：BEFORE_RECEIVE收货前/AFTER_RECEIVE收货后/BEFORE_PUTAWAY上架前 */
@Data
@TableName("wms_qc_task")
public class QcTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 质检任务号 */
    private String taskNo;

    /** 关联质检单号 */
    private String qcNo;

    /** 质检时机：BEFORE_RECEIVE收货前/AFTER_RECEIVE收货后/BEFORE_PUTAWAY上架前 */
    private String qcTiming;

    /** 质检类型：FULL全检/SAMPLE抽检/NONE免检 */
    private String qcType;

    /** 关联ASN号 */
    private String asnNo;

    /** 关联入库单号 */
    private String inboundNo;

    /** 关联入库明细号 */
    private String inboundDetailNo;

    /** 关联收货任务号 */
    private String receiptTaskNo;

    /** 关联收货任务明细号 */
    private String receiptDetailNo;

    /** 关联上架任务号 */
    private String putawayTaskNo;

    /** 关联上架任务明细号 */
    private String putawayDetailNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 商品条码 */
    private String barcode;

    /** 供应商编码 */
    private String supplierCode;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 批次号 */
    private String batchNo;

    /** 质检数量（批次数量） */
    private BigDecimal lotQty;

    /** 抽样数量 */
    private BigDecimal sampleQty;

    /** 已检验数量 */
    private BigDecimal inspectedQty;

    /** 合格数量 */
    private BigDecimal qualifiedQty;

    /** 不合格数量 */
    private BigDecimal unqualifiedQty;

    /** 质检结果：PASSED合格/FAILED不合格/CONCESSION让步接收 */
    private String qcResult;

    /** 状态：PENDING待质检/INSPECTING质检中/PASSED合格/FAILED不合格/CONCESSION让步接收/DISPOSED已处理/CANCELLED已取消 */
    private String status;

    /** 是否阻塞收货：Y/N（收货前质检不合格时阻塞收货） */
    private String blockReceive;

    /** 是否阻塞上架：Y/N（收货后质检不合格时阻塞上架） */
    private String blockPutaway;

    /** 质检员 */
    private String inspector;

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
