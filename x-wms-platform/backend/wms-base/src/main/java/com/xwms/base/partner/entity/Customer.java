package com.xwms.base.partner.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 客户档案 */
@Data
@TableName("wms_customer")
public class Customer {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String customerCode;
    private String customerName;

    /** 客户类型: B2B/B2C/CHANNEL/DISTRIBUTOR */
    private String customerType;

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

    /** 配送方式: EXPRESS/SELF/STATION */
    private String deliveryMethod;

    private String defaultWarehouse;

    /** 价格等级: VIP/GOLD/SILVER/NORMAL */
    private String priceLevel;

    /** 状态: ACTIVE/DISABLED */
    private String status;

    /** 客户扩展属性(JSON) */
    private String customerAttrs;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
