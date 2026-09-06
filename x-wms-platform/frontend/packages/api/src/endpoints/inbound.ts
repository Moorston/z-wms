import apiClient from "../client";
import type { InboundOrder, InboundOrderQuery, PageResult } from "@xwms/shared";

// 入库单API
export const inboundApi = {
  // 分页查询入库单
  page: (params: InboundOrderQuery) =>
    apiClient.get<unknown, PageResult<InboundOrder>>("/inbound/orders", { params }),

  // 获取入库单详情
  get: (id: string) => apiClient.get<unknown, InboundOrder>(`/inbound/orders/${id}`),

  // 创建入库单
  create: (data: Partial<InboundOrder>) => apiClient.post<unknown, InboundOrder>("/inbound/orders", data),

  // 更新入库单
  update: (id: string, data: Partial<InboundOrder>) =>
    apiClient.put<unknown, InboundOrder>(`/inbound/orders/${id}`, data),

  // 删除入库单
  delete: (id: string) => apiClient.delete(`/inbound/orders/${id}`),

  // 确认收货
  receive: (id: string, data: { receivedQty: number; batchNo?: string }) =>
    apiClient.post(`/inbound/orders/${id}/receive`, data),

  // 上架
  putaway: (id: string, data: { locationCode: string; qty: number }) =>
    apiClient.post(`/inbound/orders/${id}/putaway`, data),

  // 取消
  cancel: (id: string) => apiClient.post(`/inbound/orders/${id}/cancel`),
};
