package com.xwms.core.bom.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/** 产品BOM明细表 定义BOM中每个子件的数量和属性 */
@Data
@TableName("wms_product_bom_detail")
public class ProductBomDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** BOM编码 */
    private String bomCode;

    /** 行号 */
    private Integer lineNo;

    /** 子件商品编码 */
    private String childSkuCode;

    /** 子件商品名称 */
    private String childSkuName;

    /** 子件规格 */
    private String childSpec;

    /** 子件单位 */
    private String childUnit;

    /** 子件数量（一个父件需要的子件数量） */
    private BigDecimal childQty;

    /** 损耗率（百分比） */
    private BigDecimal lossRate;

    /** 是否关键件：Y/N */
    private String isKey;

    /** 可选件：Y/N（可选件可以不扫描） */
    private String isOptional;

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
