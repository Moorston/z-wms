package com.xwms.core.serial.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 序列号主表 支持1级（单品级唯一码）和2级（箱号+单品序列号）管理 */
@Data
@TableName("wms_serial_number")
public class SerialNumber {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 序列号（唯一） */
    private String serialNo;

    /** 序列号级别：1=单品级/2=箱级 */
    private Integer serialLevel;

    /** 父序列号（2级时关联箱号） */
    private String parentSerialNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 商品条码 */
    private String barcode;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 批次号 */
    private String batchNo;

    /** 入库单号 */
    private String inboundNo;

    /** ASN号 */
    private String asnNo;

    /** 出库单号 */
    private String outboundNo;

    /** 波次号 */
    private String waveNo;

    /** 当前库位 */
    private String locationCode;

    /** 状态：IN_STOCK在库/OUT_STOCK已出库/RETURNED退货/SCRAPPED报废/FROZEN冻结 */
    private String status;

    /** 序列号规则编码 */
    private String ruleCode;

    /** 生产日期 */
    private LocalDateTime productionDate;

    /** 失效日期 */
    private LocalDateTime expiryDate;

    /** 入库时间 */
    private LocalDateTime inboundTime;

    /** 出库时间 */
    private LocalDateTime outboundTime;

    /** 供应商编码 */
    private String supplierCode;

    /** 客户编码 */
    private String customerCode;

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
