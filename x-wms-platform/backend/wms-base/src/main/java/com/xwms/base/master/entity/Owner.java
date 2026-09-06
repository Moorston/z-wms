package com.xwms.base.master.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.xwms.common.core.BaseEntity;
import com.xwms.common.crypto.EncryptTypeHandler;
import com.xwms.common.crypto.Sensitive;
import com.xwms.common.crypto.SensitiveSerializer;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 货主档案实体 3PL模式下，一个WMS服务多个货主
 *
 * <p>敏感字段加密：contact/phone/address
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wms_owner", autoResultMap = true)
public class Owner extends BaseEntity {
    /** 货主编码 */
    private String ownerCode;

    /** 货主名称 */
    private String ownerName;

    /** 货主类型：MANUFACTURER/DISTRIBUTOR/RETAILER/ECOMMERCE */
    private String ownerType;

    /** 联系人（加密存储+脱敏展示） */
    @Sensitive(type = Sensitive.SensitiveType.NAME)
    @JsonSerialize(using = SensitiveSerializer.class)
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String contact;

    /** 联系电话（加密存储+脱敏展示） */
    @Sensitive(type = Sensitive.SensitiveType.PHONE)
    @JsonSerialize(using = SensitiveSerializer.class)
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String phone;

    /** 手机号检索哈希 */
    private String phoneHash;

    /** 地址（加密存储+脱敏展示） */
    @Sensitive(type = Sensitive.SensitiveType.ADDRESS)
    @JsonSerialize(using = SensitiveSerializer.class)
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String address;

    /** 行业：PHARMA/FOOD/ECOMMERCE/GENERAL */
    private String industry;

    /** 计费方式 */
    private String billingType;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 货主配置（JSON，如行业插件启用/规则配置） */
    private String ownerConfig;
}
