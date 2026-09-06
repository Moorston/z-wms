import apiClient from "../client";
import type { Inventory, InventoryQuery, InventoryTransaction, PageResult } from "@xwms/shared";

// 库存API
export const inventoryApi = {
  // 库存余量查询
  page: (params: InventoryQuery) =>
    apiClient.get<unknown, PageResult<Inventory>>("/inventory/stocks", { params }),

  // 库存详情
  get: (id: string) => apiClient.get<unknown, Inventory>(`/inventory/stocks/${id}`),

  // 库存事务流水
  transactions: (params: { sku?: string; locationCode?: string; page?: number; size?: number }) =>
    apiClient.get<unknown, PageResult<InventoryTransaction>>("/inventory/transactions", { params }),

  // 库存移动
  move: (data: { fromLocation: string; toLocation: string; sku: string; qty: number; batchNo?: string }) =>
    apiClient.post("/inventory/move", data),

  // 库存调整
  adjust: (data: { locationCode: string; sku: string; adjustQty: number; reason: string; batchNo?: string }) =>
    apiClient.post("/inventory/adjust", data),

  // 库存冻结
  freeze: (data: { locationCode: string; sku: string; qty: number; reason: string; batchNo?: string }) =>
    apiClient.post("/inventory/freeze", data),

  // 库存解冻
  unfreeze: (id: string) => apiClient.post(`/inventory/freeze/${id}/unfreeze`),

  // 盘点
  stocktake: {
    create: (data: any) => apiClient.post("/inventory/stocktakes", data),
    page: (params: any) => apiClient.get("/inventory/stocktakes", { params }),
    confirm: (id: string, data: any) => apiClient.post(`/inventory/stocktakes/${id}/confirm`),
  },

  // 批次追踪
  batchTrace: (batchNo: string) => apiClient.get(`/inventory/batch-trace/${batchNo}`),
};
