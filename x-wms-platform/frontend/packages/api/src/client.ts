import axios, { AxiosError, AxiosInstance, InternalAxiosRequestConfig } from "axios";

// API响应统一格式
export interface ApiResult<T = any> {
  code: number;
  message: string;
  data: T;
}

// 创建Axios实例
const apiClient: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 30000,
  headers: {
    "Content-Type": "application/json",
  },
});

// 请求拦截器：添加Token和多货主标识
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem("access_token");
    const ownerCode = localStorage.getItem("owner_code");

    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    if (ownerCode) {
      config.headers["X-Owner-Code"] = ownerCode;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 响应拦截器：统一处理错误和Token刷新
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

apiClient.interceptors.response.use(
  (response) => {
    const result = response.data as ApiResult;
    // 业务成功
    if (result.code === 200 || result.code === 0) {
      return result.data;
    }
    // 业务错误
    return Promise.reject(new Error(result.message || "请求失败"));
  },
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // 401未授权，尝试刷新Token
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve) => {
          subscribeTokenRefresh((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(apiClient(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const refreshToken = localStorage.getItem("refresh_token");
        const response = await axios.post("/api/auth/refresh", { refreshToken });
        const { accessToken } = response.data.data;
        localStorage.setItem("access_token", accessToken);
        onRefreshed(accessToken);
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch {
        localStorage.clear();
        window.location.href = "/login";
        return Promise.reject(error);
      } finally {
        isRefreshing = false;
      }
    }

    // 403无权限
    if (error.response?.status === 403) {
      console.error("无权限访问");
    }

    return Promise.reject(error);
  }
);

export default apiClient;
