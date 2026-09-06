package com.xwms.base.partner.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 货主档案 */
@Data
@TableName("wms_owner")
public class Owner {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ownerCode;
    private String ownerName;

    /** 货主类型: ENTERPRISE/INDIVIDUAL/PLATFORM */
    private String ownerType;

    private String shortName;
    private String legalPerson;
    private String businessLicense;
    private String taxNumber;
    private String contact;
    private String phone;
    private String email;
    private String address;
    private String country;
    private String province;
    private String city;
    private String district;
    private String postcode;

    /** 结算方式: MONTHLY/WEEKLY/DAILY/PREPAY */
    private String settlementType;

    private BigDecimal creditLimit;
    private BigDecimal creditUsed;

    /** 状态: ACTIVE/DISABLED/FROZEN */
    private String status;

    /** 货主扩展属性(JSON) */
    private String ownerAttrs;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
