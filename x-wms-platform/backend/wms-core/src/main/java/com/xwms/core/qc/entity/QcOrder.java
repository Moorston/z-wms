package com.xwms.core.qc.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质检单 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_qc_order")
public class QcOrder extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 质检单号 */
    private String qcNo;

    /** 关联单据类型: INBOUND入库/RETURN退货/INSTOCK在库/OUTBOUND出库 */
    private String refType;

    /** 关联单据号 */
    private String refNo;

    /** 关联明细行ID */
    private Long refItemId;

    /** SKU编码 */
    private String sku;

    /** 条码 */
    private String barcode;

    /** 商品名称 */
    private String productName;

    /** 供应商编码 */
    private String supplierCode;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 库位编码 */
    private String locationCode;

    /** 批次号 */
    private String batchNo;

    /** 质检类型: FULL全检/SAMPLE抽检/NONE免检 */
    private String qcType;

    /** 批量 */
    private BigDecimal lotQty;

    /** 抽样数量 */
    private BigDecimal sampleQty;

    /** 已检数量 */
    private BigDecimal inspectedQty;

    /** 合格数量 */
    private BigDecimal qualifiedQty;

    /** 不合格数量 */
    private BigDecimal unqualifiedQty;

    /** AQL水平 */
    private String aqlLevel;

    /** 接收数 */
    private Integer acceptNumber;

    /** 拒收数 */
    private Integer rejectNumber;

    /** 不良数 */
    private Integer defectCount;

    /** 质检结果: PASSED/FAILED/CONCESSION */
    private String result;

    /** 状态: PENDING/INSPECTING/PASSED/FAILED/CONCESSION_PENDING/CONCESSION_APPROVED/DISPOSED */
    private String status;

    /** 检验员 */
    private String inspector;

    /** 复核员(GSP双人) */
    private String inspector2;

    /** 检验时间 */
    private LocalDateTime inspectTime;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 备注 */
    private String remark;

    /** 多租户货主隔离字段 */
    @TableField("owner_code_col")
    private String ownerCodeCol;

    /** 多租户仓库隔离字段 */
    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
