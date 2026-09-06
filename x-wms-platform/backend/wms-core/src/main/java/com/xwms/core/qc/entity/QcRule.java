package com.xwms.core.qc.entity;

import com.baomidou.mybatisplus.annotation.*;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 质检规则 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_qc_rule")
public class QcRule extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** SKU编码(为空表示通用) */
    private String sku;

    /** 供应商编码(为空表示通用) */
    private String supplierCode;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouseCode;

    /** 质检类型: FULL/SAMPLE/NONE */
    private String qcType;

    /** AQL水平 */
    private String aqlLevel;

    /** 检验水平: S-1/S-2/S-3/S-4/I/II/III */
    private String inspectionLevel;

    /** 严格度: NORMAL/REDUCED/TIGHTENED */
    private String strictness;

    /** 新供应商前N批全检 */
    private Integer firstBatchFull;

    /** 全检批次数 */
    private Integer fullCheckCount;

    /** 状态: ENABLED/DISABLED */
    private String status;

    /** 备注 */
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;
}
