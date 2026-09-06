package com.xwms.base.batch.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 批次属性值 */
@Data
@TableName("wms_batch_value")
public class BatchValue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 批号 */
    private String batchNo;

    /** SKU编码 */
    private String skuCode;

    private String warehouseCode;

    /** 属性编码 */
    private String attrCode;

    private String attrName;

    /** 属性值(字符串) */
    private String attrValue;

    /** 属性值(日期) */
    private LocalDateTime attrValueDate;

    /** 属性值(数字) */
    private BigDecimal attrValueNum;

    /** 来源类型: MANUAL/SCAN/IMPORT/SYSTEM */
    private String sourceType;

    /** 来源单号 */
    private String sourceRef;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
