package com.xwms.common.security.dto;

import lombok.Data;

/** 登录请求DTO */
@Data
public class LoginRequest {
    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 仓库编码（多仓库用户选择） */
    private String warehouse;
}
