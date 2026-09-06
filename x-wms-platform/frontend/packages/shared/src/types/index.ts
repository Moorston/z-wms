// 分页结果
export interface PageResult<T> {
  records: T[];
  total: number;
  page: number;
  size: number;
}

// 分页查询参数
export interface PageQuery {
  page?: number;
  size?: number;
  keyword?: string;
}

// 入库单
export interface InboundOrder {
  id: string;
  orderNo: string;
  orderType: string; // PO/ASN/RETURN
  status: string; // CREATED/RECEIVED/PUTAWAY/COMPLETED/CANCELLED
  ownerCode: string;
  warehouseCode: string;
  supplierCode?: string;
  expectedDate: string;
  actualDate?: string;
  totalQty: number;
  receivedQty: number;
  putawayQty: number;
  remark?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  items?: InboundOrderItem[];
}

export interface InboundOrderItem {
  id: string;
  orderId: string;
  sku: string;
  productName: string;
  expectedQty: number;
  receivedQty: number;
  putawayQty: number;
  batchNo?: string;
  unit: string;
}

export interface InboundOrderQuery extends PageQuery {
  orderNo?: string;
  status?: string;
  orderType?: string;
  ownerCode?: string;
  warehouseCode?: string;
  startDate?: string;
  endDate?: string;
}

// 出库单
export interface OutboundOrder {
  id: string;
  orderNo: string;
  orderType: string; // SO/TRANSFER/CROSSDOCK
  status: string; // CREATED/ALLOCATED/PICKED/REVIEWED/SHIPPED/CANCELLED
  ownerCode: string;
  warehouseCode: string;
  customerCode?: string;
  waveId?: string;
  expectedShipDate: string;
  actualShipDate?: string;
  totalQty: number;
  allocatedQty: number;
  pickedQty: number;
  shippedQty: number;
  carrier?: string;
  expressNo?: string;
  remark?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
  items?: OutboundOrderItem[];
}

export interface OutboundOrderItem {
  id: string;
  orderId: string;
  sku: string;
  productName: string;
  orderedQty: number;
  allocatedQty: number;
  pickedQty: number;
  shippedQty: number;
  batchNo?: string;
  locationCode?: string;
  unit: string;
}

export interface OutboundOrderQuery extends PageQuery {
  orderNo?: string;
  status?: string;
  orderType?: string;
  ownerCode?: string;
  warehouseCode?: string;
  customerCode?: string;
  startDate?: string;
  endDate?: string;
}

// 波次
export interface WaveOrder {
  id: string;
  waveNo: string;
  status: string; // CREATED/RELEASED/PICKING/COMPLETED/CANCELLED
  warehouseCode: string;
  ownerCode: string;
  orderCount: number;
  totalQty: number;
  pickedQty: number;
  picker?: string;
  releasedAt?: string;
  completedAt?: string;
  createdAt: string;
}

// 库存
export interface Inventory {
  id: string;
  ownerCode: string;
  warehouseCode: string;
  locationCode: string;
  sku: string;
  productName: string;
  batchNo?: string;
  qty: number;
  availableQty: number;
  allocatedQty: number;
  frozenQty: number;
  unit: string;
  updatedAt: string;
}

export interface InventoryQuery extends PageQuery {
  ownerCode?: string;
  warehouseCode?: string;
  locationCode?: string;
  sku?: string;
  batchNo?: string;
}

// 库存事务
export interface InventoryTransaction {
  id: string;
  ownerCode: string;
  warehouseCode: string;
  locationCode: string;
  sku: string;
  batchNo?: string;
  transactionType: string; // INBOUND/OUTBOUND/MOVE/ADJUST/FREEZE/UNFREEZE
  qtyChange: number;
  beforeQty: number;
  afterQty: number;
  refOrderNo?: string;
  refOrderType?: string;
  remark?: string;
  operatedBy: string;
  operatedAt: string;
}

// 产品
export interface Product {
  id: string;
  sku: string;
  productName: string;
  category?: string;
  unit: string;
  weight?: number;
  volume?: number;
  barcode?: string;
  spec?: string;
  status: string;
  ownerCode: string;
  createdAt: string;
}

// 客户
export interface Customer {
  id: string;
  customerCode: string;
  customerName: string;
  contact?: string;
  phone?: string;
  address?: string;
  status: string;
  ownerCode: string;
}

// 货主
export interface Owner {
  id: string;
  ownerCode: string;
  ownerName: string;
  contact?: string;
  phone?: string;
  status: string;
}

// 仓库
export interface Warehouse {
  id: string;
  warehouseCode: string;
  warehouseName: string;
  address?: string;
  area?: number;
  status: string;
}

// 库位
export interface Location {
  id: string;
  locationCode: string;
  warehouseCode: string;
  areaCode?: string;
  locationType: string; // STORAGE/PICKING/RECEIVING/SHIPPING
  status: string; // EMPTY/OCCUPIED/FROZEN/DISABLED
  capacity?: number;
  usedCapacity?: number;
  lane?: string;
  row?: string;
  column?: string;
  level?: string;
}

// 用户
export interface User {
  id: string;
  username: string;
  realName: string;
  role: string;
  ownerCode?: string;
  warehouseCode?: string;
  status: string;
}
