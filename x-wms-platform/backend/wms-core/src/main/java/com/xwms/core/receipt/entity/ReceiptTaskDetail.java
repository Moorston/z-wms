package com.xwms.core.receipt.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 收货任务明细表 */
@Data
@TableName("wms_receipt_task_detail")
public class ReceiptTaskDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 明细号 */
    private String detailNo;

    /** 收货任务号 */
    private String taskNo;

    /** 行号 */
    private Integer lineNo;

    /** 关联ASN行号 */
    private Integer asnLineNo;

    /** 关联入库明细号 */
    private String inboundDetailNo;

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

    /** 预期数量 */
    private BigDecimal expectedQty;

    /** 已收货数量 */
    private BigDecimal receivedQty;

    /** 差异数量 */
    private BigDecimal differenceQty;

    /** 批次号 */
    private String batchNo;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

    /** 序列号（单个商品时） */
    private String serialNo;

    /** 是否需要批次管理：Y/N */
    private String batchManaged;

    /** 是否需要序列号管理：Y/N */
    private String serialManaged;

    /** 是否需要效期管理：Y/N */
    private String expiryManaged;

    /** 是否需要质检：Y/N */
    private String qcRequired;

    /** 收货库位 */
    private String receiveLocation;

    /** 状态：PENDING/RECEIVING/PARTIAL/COMPLETED/CANCELLED */
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
