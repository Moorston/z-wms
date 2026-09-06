package com.xwms.core.qc.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 质检明细 */
@Data
@TableName("wms_qc_item")
public class QcItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 质检单ID */
    private Long qcId;

    /** 检验项编码 */
    private String itemCode;

    /** 检验项名称 */
    private String itemName;

    /** 分类: APPEARANCE/QUANTITY/SPEC/EXPIRY/FUNCTION/PACKAGING/TEMPERATURE/OTHER */
    private String category;

    /** 标准值 */
    private String standard;

    /** 实际值 */
    private String actualValue;

    /** 单位 */
    private String unit;

    /** 结果: PASS/FAIL/NA */
    private String result;

    /** 严重程度: CRITICAL/MAJOR/MINOR */
    private String severity;

    /** 备注 */
    private String remark;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
