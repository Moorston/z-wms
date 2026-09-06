import apiClient from "../client";

// 认证API
export const authApi = {
  // 登录
  login: (data: { username: string; password: string }) =>
    apiClient.post<unknown, { accessToken: string; refreshToken: string; user: any }>("/auth/login", data),

  // 刷新Token
  refresh: (refreshToken: string) =>
    apiClient.post<unknown, { accessToken: string }>("/auth/refresh", { refreshToken }),

  // 登出
  logout: () => apiClient.post("/auth/logout"),

  // 获取当前用户信息
  me: () => apiClient.get("/auth/me"),

  // 修改密码
  changePassword: (data: { oldPassword: string; newPassword: string }) =>
    apiClient.post("/auth/change-password", data),
};
