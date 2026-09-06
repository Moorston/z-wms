package com.xwms.base.location.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 仓库实体 顶层组织单元，一个WMS可管理多个仓库 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wms_warehouse")
public class Warehouse extends BaseEntity {
    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 仓库类型：CENTER/FRONT/RETURN */
    private String warehouseType;

    /** 地址 */
    private String address;

    /** 联系人 */
    private String contact;

    /** 联系电话 */
    private String phone;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 仓库属性（JSON，如温区/面积/容量） */
    private String warehouseAttrs;
}
