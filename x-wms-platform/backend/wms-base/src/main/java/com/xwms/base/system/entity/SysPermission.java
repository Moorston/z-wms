package com.xwms.base.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统权限实体 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    /** 权限编码 */
    private String permCode;

    /** 权限名称 */
    private String permName;

    /** 权限类型：MENU/BUTTON/API */
    private String permType;

    /** 父权限ID */
    private Long parentId;

    /** 路径（菜单URL/API路径） */
    private String path;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sortNo;

    /** 状态：ACTIVE/DISABLED */
    private String status;
}
