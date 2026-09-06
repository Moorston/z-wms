package com.xwms.base.carrier.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 承运商档案 */
@Data
@TableName("wms_carrier")
public class Carrier {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String carrierCode;
    private String carrierName;

    /** 承运商类型: EXPRESS/LOGISTICS/SELF */
    private String carrierType;

    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String apiUrl;

    /** API密钥(加密存储) */
    private String apiKey;

    /** API密钥(加密存储) */
    private String apiSecret;

    private String customerCode;

    /** 状态: ACTIVE/INACTIVE */
    private String status;

    private Integer priority;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
