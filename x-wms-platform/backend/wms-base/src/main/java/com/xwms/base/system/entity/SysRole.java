package com.xwms.base.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统角色实体 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {
    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 角色描述 */
    private String description;

    /** 状态：ACTIVE/DISABLED */
    private String status;

    /** 排序号 */
    private Integer sortNo;
}
