package com.xwms.common.security.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** 当前用户工具类 从SecurityContext中获取当前登录用户信息 */
public class CurrentUserUtil {

    /** 获取当前用户名 */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }

    /** 判断当前用户是否有指定角色 */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return false;
        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(roleWithPrefix));
    }

    /** 判断是否为管理员 */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
