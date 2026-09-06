package com.xwms.core.putawayrule.dto;

import java.math.BigDecimal;

import lombok.Data;

/** 空间限制DTO 6种限制类型：VOLUME体积/WEIGHT重量/QUANTITY数量/PALLET托盘数/CASE箱数/DIMENSION尺寸 */
@Data
public class SpaceLimit {

    /** 限制类型 */
    private String type;

    /** 阈值（体积m³/重量kg/数量/托盘数/箱数） */
    private BigDecimal threshold;

    /** 尺寸限制-长(mm)（DIMENSION类型用） */
    private BigDecimal maxLength;

    /** 尺寸限制-宽(mm)（DIMENSION类型用） */
    private BigDecimal maxWidth;

    /** 尺寸限制-高(mm)（DIMENSION类型用） */
    private BigDecimal maxHeight;
}
