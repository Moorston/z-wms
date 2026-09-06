package com.xwms.core.plugin.industry.gsp.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** GSP批次记录 药品经营质量管理规范(GSP)批次追溯记录 */
@Data
@TableName("wms_gsp_batch_record")
public class GspBatchRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 记录编号 */
    private String recordNo;

    /** 批次号 */
    private String batchNo;

    /** 商品编码 */
    private String skuCode;

    /** 商品名称 */
    private String skuName;

    /** 规格 */
    private String spec;

    /** 剂型 */
    private String dosageForm;

    /** 生产厂家 */
    private String manufacturer;

    /** 批准文号 */
    private String approvalNumber;

    /** 生产日期 */
    private LocalDate productionDate;

    /** 有效期至 */
    private LocalDate expiryDate;

    /** 入库数量 */
    private BigDecimal inboundQty;

    /** 库存数量 */
    private BigDecimal stockQty;

    /** 已出库数量 */
    private BigDecimal outboundQty;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库区编码 */
    private String areaCode;

    /** 库位编码 */
    private String locationCode;

    /** 货主编码 */
    private String ownerCode;

    /** 供应商编码 */
    private String supplierCode;

    /** 供应商名称 */
    private String supplierName;

    /** 采购单号 */
    private String purchaseOrderNo;

    /** 入库单号 */
    private String inboundNo;

    /** 收货单号 */
    private String receiveNo;

    /** 验收单号 */
    private String acceptanceNo;

    /** 质量状态: QUALIFIED合格/UNQUALIFIED不合格/WAITING待验/RETURNING退货中 */
    private String qualityStatus;

    /** 养护状态: NORMAL正常/MAINTAINING养护中/EXPIRED已过期/NEAR_EXPIRY近效期 */
    private String maintenanceStatus;

    /** 储存条件: COOL阴凉/COLD冷藏/FROZEN冷冻/NORMAL常温 */
    private String storageCondition;

    /** 温度要求(℃) */
    private BigDecimal tempRequirement;

    /** 湿度要求(%) */
    private BigDecimal humidityRequirement;

    /** 是否首营品种: Y是/N否 */
    private String isFirstVariety;

    /** 是否进口药品: Y是/N否 */
    private String isImported;

    /** 是否特殊管理药品: Y是/N否 */
    private String isSpecialManaged;

    /** 特殊药品类型: NARCOTIC麻醉/PSYCHOTIC精神/TOXIC毒性/RADIOACTIVE放射性 */
    private String specialDrugType;

    /** 追溯码(药品电子监管码) */
    private String traceCode;

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
