package com.xwms.common.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 登录响应DTO */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    /** Access Token */
    private String accessToken;

    /** Refresh Token */
    private String refreshToken;

    /** Token类型 */
    private String tokenType;

    /** Access Token过期时间（秒） */
    private Long expiresIn;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 角色列表 */
    private String roles;

    /** 所属仓库 */
    private String warehouse;
}
