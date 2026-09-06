package com.xwms.common.security.dto;

import lombok.Data;

/** 刷新Token请求DTO */
@Data
public class RefreshTokenRequest {
    private String refreshToken;
}
