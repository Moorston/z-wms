package com.xwms.base.partner.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 联系人档案 */
@Data
@TableName("wms_contact")
public class Contact {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String contactCode;
    private String contactName;

    /** 合作伙伴类型: OWNER/CUSTOMER/SUPPLIER */
    private String partnerType;

    private String partnerCode;
    private String position;
    private String department;
    private String phone;
    private String mobile;
    private String email;
    private String wechat;

    /** 是否主联系人 */
    private Integer isPrimary;

    /** 状态: ACTIVE/DISABLED */
    private String status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
