import apiClient from "../client";
import type { Product, Customer, Warehouse, Location, Owner, PageResult } from "@xwms/shared";

// 基础数据API
export const masterApi = {
  // 产品档案
  product: {
    page: (params: any) => apiClient.get<unknown, PageResult<Product>>("/master/products", { params }),
    get: (id: string) => apiClient.get<unknown, Product>(`/master/products/${id}`),
    create: (data: Partial<Product>) => apiClient.post("/master/products", data),
    update: (id: string, data: Partial<Product>) => apiClient.put(`/master/products/${id}`, data),
    delete: (id: string) => apiClient.delete(`/master/products/${id}`),
  },

  // 客户档案
  customer: {
    page: (params: any) => apiClient.get<unknown, PageResult<Customer>>("/master/customers", { params }),
    get: (id: string) => apiClient.get<unknown, Customer>(`/master/customers/${id}`),
    create: (data: Partial<Customer>) => apiClient.post("/master/customers", data),
    update: (id: string, data: Partial<Customer>) => apiClient.put(`/master/customers/${id}`, data),
    delete: (id: string) => apiClient.delete(`/master/customers/${id}`),
  },

  // 货主档案
  owner: {
    page: (params: any) => apiClient.get<unknown, PageResult<Owner>>("/master/owners", { params }),
    get: (id: string) => apiClient.get<unknown, Owner>(`/master/owners/${id}`),
    create: (data: Partial<Owner>) => apiClient.post("/master/owners", data),
    update: (id: string, data: Partial<Owner>) => apiClient.put(`/master/owners/${id}`, data),
  },

  // 仓库
  warehouse: {
    page: (params: any) => apiClient.get<unknown, PageResult<Warehouse>>("/master/warehouses", { params }),
    get: (id: string) => apiClient.get<unknown, Warehouse>(`/master/warehouses/${id}`),
    create: (data: Partial<Warehouse>) => apiClient.post("/master/warehouses", data),
    update: (id: string, data: Partial<Warehouse>) => apiClient.put(`/master/warehouses/${id}`, data),
  },

  // 库位
  location: {
    page: (params: any) => apiClient.get<unknown, PageResult<Location>>("/master/locations", { params }),
    get: (id: string) => apiClient.get<unknown, Location>(`/master/locations/${id}`),
    create: (data: Partial<Location>) => apiClient.post("/master/locations", data),
    update: (id: string, data: Partial<Location>) => apiClient.put(`/master/locations/${id}`, data),
    // 图形化查询
    graphic: (warehouseCode: string) => apiClient.get(`/master/locations/graphic/${warehouseCode}`),
  },
};
