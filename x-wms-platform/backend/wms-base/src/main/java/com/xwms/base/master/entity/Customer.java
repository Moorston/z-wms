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
 * 客户档案实体 出库收货方
 *
 * <p>敏感字段加密： - contact/phone/shippingAddress 加密存储 - phoneHash 用于精确查询（SHA-256哈希） - JSON序列化时自动脱敏
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "wms_customer", autoResultMap = true)
public class Customer extends BaseEntity {
    /** 客户编码 */
    private String customerCode;

    /** 客户名称 */
    private String customerName;

    /** 货主（客户属于哪个货主） */
    private String ownerCode;

    /** 客户类型：B2B/B2C/STORE */
    private String customerType;

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

    /** 手机号检索哈希（SHA-256，用于精确查询） */
    private String phoneHash;

    /** 收货地址（加密存储+脱敏展示） */
    @Sensitive(type = Sensitive.SensitiveType.ADDRESS)
    @JsonSerialize(using = SensitiveSerializer.class)
    @TableField(typeHandler = EncryptTypeHandler.class)
    private String shippingAddress;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区 */
    private String district;

    /** 默认快递 */
    private String defaultExpress;

    /** 状态：ACTIVE/DISABLED */
    private String status;
}
