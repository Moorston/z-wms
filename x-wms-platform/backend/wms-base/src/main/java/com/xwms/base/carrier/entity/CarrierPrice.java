package com.xwms.base.carrier.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 承运商价格 */
@Data
@TableName("wms_carrier_price")
public class CarrierPrice {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String carrierCode;
    private String serviceCode;

    /** 区域编码 */
    private String regionCode;

    private BigDecimal startWeight;
    private BigDecimal endWeight;
    private BigDecimal firstWeight;
    private BigDecimal firstPrice;
    private BigDecimal additionalWeight;
    private BigDecimal additionalPrice;
    private BigDecimal baseFee;

    /** 燃油附加费(百分比) */
    private BigDecimal fuelSurcharge;

    private BigDecimal otherFee;
    private LocalDateTime effectiveDate;
    private LocalDateTime expireDate;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
