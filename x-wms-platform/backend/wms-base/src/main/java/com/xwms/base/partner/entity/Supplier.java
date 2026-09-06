package com.xwms.base.partner.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 供应商档案 */
@Data
@TableName("wms_supplier")
public class Supplier {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String supplierCode;
    private String supplierName;

    /** 供应商类型: MANUFACTURER/DISTRIBUTOR/AGENT */
    private String supplierType;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    private String shortName;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String country;
    private String province;
    private String city;
    private String district;
    private String postcode;

    /** 结算方式 */
    private String settlementType;

    /** 付款条件: NET30/NET60/CASH */
    private String paymentTerm;

    /** 状态: ACTIVE/DISABLED */
    private String status;

    /** 供应商扩展属性(JSON) */
    private String supplierAttrs;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
