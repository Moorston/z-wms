package com.xwms.core.batch.entity;

import java.time.LocalDate;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 批次实体 WMS批次全链路追踪的核心，每个SKU+批号唯一 批次属性：生产日期/有效期/供应商/质检状态/温区等 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_batch")
public class Batch extends BaseEntity {
    /** 批号 */
    private String batchNo;

    /** SKU */
    private String sku;

    /** 货主 */
    private String ownerCode;

    /** 仓库 */
    private String warehouse;

    /** 生产日期 */
    private LocalDate produceDate;

    /** 有效期 */
    private LocalDate expireDate;

    /** 供应商批号 */
    private String supplierBatchNo;

    /** 质检状态：PENDING/PASSED/FAILED */
    private String qcStatus;

    /** 质检单号 */
    private String qcNo;

    /** 温区要求：FROZEN/CHILLED/CONSTANT/NORMAL */
    private String temperatureZone;

    /** 批次状态：ACTIVE/FROZEN/QUARANTINE/EXPIRED */
    private String status;

    /** 初始数量 */
    private java.math.BigDecimal initialQty;

    /** 当前数量 */
    private java.math.BigDecimal currentQty;

    /** 来源入库单号 */
    private String sourceInboundNo;

    /** 扩展属性（JSON，批次自定义属性） */
    private String batchAttrs;
}
