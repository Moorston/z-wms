package com.xwms.base.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import com.xwms.common.core.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 系统用户实体 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    /** 用户名 */
    private String username;

    /** 密码（BCrypt加密） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 所属仓库 */
    private String warehouse;

    /** 部门 */
    private String department;

    /** 状态：ACTIVE/DISABLED/LOCKED */
    private String status;

    /** 最后登录时间 */
    private java.time.LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 备注 */
    private String remark;
}
