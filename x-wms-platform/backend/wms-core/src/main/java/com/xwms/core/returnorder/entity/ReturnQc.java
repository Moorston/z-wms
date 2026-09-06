package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 退货质检 */
@Data
@TableName("wms_return_qc")
public class ReturnQc {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long returnId;
    private Long returnItemId;
    private String sku;
    private String batchNo;

    private BigDecimal qty;

    /** 质检类型: FULL全检/SAMPLE抽检 */
    private String qcType;

    private BigDecimal sampleQty;
    private BigDecimal inspectedQty;
    private BigDecimal qualifiedQty;
    private BigDecimal unqualifiedQty;

    /** 质检结果: PASSED/FAILED/CONCESSION */
    private String qcResult;

    /** 缺陷类型: APPEARANCE/FUNCTION/PACKAGING/EXPIRE/DAMAGE */
    private String defectType;

    private String defectDesc;

    private String inspector;
    private LocalDateTime qcTime;

    /** 处理方式: RETURN_TO_CUSTOMER/DESTROY/REPAIR/REPACKAGE/SECONDHAND */
    private String handleMethod;

    /** 处理状态: PENDING/PROCESSING/COMPLETED */
    private String handleStatus;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
