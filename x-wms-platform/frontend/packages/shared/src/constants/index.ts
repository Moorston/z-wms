// 单据状态枚举
export const ORDER_STATUS = {
  CREATED: "CREATED",
  PROCESSING: "PROCESSING",
  COMPLETED: "COMPLETED",
  CANCELLED: "CANCELLED",
  SHIPPED: "SHIPPED",
  PENDING: "PENDING",
  EXCEPTION: "EXCEPTION",
  ALLOCATED: "ALLOCATED",
  PICKED: "PICKED",
  RECEIVED: "RECEIVED",
  PUTAWAY: "PUTAWAY",
} as const;

// 入库单类型
export const INBOUND_ORDER_TYPE = {
  PO: "PO", // 采购入库
  ASN: "ASN", // 预约入库
  RETURN: "RETURN", // 退货入库
  TRANSFER: "TRANSFER", // 调拨入库
} as const;

// 出库单类型
export const OUTBOUND_ORDER_TYPE = {
  SO: "SO", // 销售出库
  TRANSFER: "TRANSFER", // 调拨出库
  CROSSDOCK: "CROSSDOCK", // 越库出库
} as const;

// 库存事务类型
export const INVENTORY_TRANSACTION_TYPE = {
  INBOUND: "INBOUND",
  OUTBOUND: "OUTBOUND",
  MOVE: "MOVE",
  ADJUST: "ADJUST",
  FREEZE: "FREEZE",
  UNFREEZE: "UNFREEZE",
} as const;

// 库位类型
export const LOCATION_TYPE = {
  STORAGE: "STORAGE", // 存储区
  PICKING: "PICKING", // 拣货区
  RECEIVING: "RECEIVING", // 收货区
  SHIPPING: "SHIPPING", // 发货区
} as const;

// API路径常量
export const API_PATHS = {
  AUTH: "/auth",
  INBOUND: "/inbound",
  OUTBOUND: "/outbound",
  INVENTORY: "/inventory",
  MASTER: "/master",
  SYSTEM: "/system",
} as const;

// 字典类型编码
export const DICT_TYPES = {
  ORDER_STATUS: "order_status",
  INBOUND_TYPE: "inbound_type",
  OUTBOUND_TYPE: "outbound_type",
  LOCATION_TYPE: "location_type",
  PRODUCT_CATEGORY: "product_category",
} as const;
