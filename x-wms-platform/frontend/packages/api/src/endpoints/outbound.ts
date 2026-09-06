import apiClient from "../client";
import type { OutboundOrder, OutboundOrderQuery, PageResult, WaveOrder } from "@xwms/shared";

// 出库单API
export const outboundApi = {
  page: (params: OutboundOrderQuery) =>
    apiClient.get<unknown, PageResult<OutboundOrder>>("/outbound/orders", { params }),

  get: (id: string) => apiClient.get<unknown, OutboundOrder>(`/outbound/orders/${id}`),

  create: (data: Partial<OutboundOrder>) =>
    apiClient.post<unknown, OutboundOrder>("/outbound/orders", data),

  update: (id: string, data: Partial<OutboundOrder>) =>
    apiClient.put<unknown, OutboundOrder>(`/outbound/orders/${id}`, data),

  delete: (id: string) => apiClient.delete(`/outbound/orders/${id}`),

  // 分配库存
  allocate: (id: string) => apiClient.post(`/outbound/orders/${id}/allocate`),

  // 确认拣货
  confirmPick: (id: string, data: { pickedQty: number }) =>
    apiClient.post(`/outbound/orders/${id}/pick`, data),

  // 复核
  review: (id: string) => apiClient.post(`/outbound/orders/${id}/review`),

  // 发货
  ship: (id: string, data: { expressNo?: string; carrier?: string }) =>
    apiClient.post(`/outbound/orders/${id}/ship`, data),

  // 取消
  cancel: (id: string) => apiClient.post(`/outbound/orders/${id}/cancel`),

  // 波次管理
  wave: {
    page: (params: any) => apiClient.get<unknown, PageResult<WaveOrder>>("/outbound/waves", { params }),
    create: (data: any) => apiClient.post("/outbound/waves", data),
    release: (id: string) => apiClient.post(`/outbound/waves/${id}/release`),
    cancel: (id: string) => apiClient.post(`/outbound/waves/${id}/cancel`),
  },
};
