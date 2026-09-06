// 共享类型
export type {
  PageResult,
  PageQuery,
  InboundOrder,
  InboundOrderItem,
  InboundOrderQuery,
  OutboundOrder,
  OutboundOrderItem,
  OutboundOrderQuery,
  WaveOrder,
  Inventory,
  InventoryQuery,
  InventoryTransaction,
  Product,
  Customer,
  Owner,
  Warehouse,
  Location,
  User,
} from "./types";

// 共享常量
export {
  ORDER_STATUS,
  INBOUND_ORDER_TYPE,
  OUTBOUND_ORDER_TYPE,
  INVENTORY_TRANSACTION_TYPE,
  LOCATION_TYPE,
  API_PATHS,
  DICT_TYPES,
} from "./constants";

// 共享工具
export { validateBarcode, storage, hasPermission, formatFileSize } from "./utils";
