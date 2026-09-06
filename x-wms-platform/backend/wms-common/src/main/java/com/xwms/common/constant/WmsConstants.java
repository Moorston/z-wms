package com.xwms.common.constant;

/** WMS系统常量 */
public final class WmsConstants {

    private WmsConstants() {}

    /** 库存状态 */
    public static final class InventoryStatus {
        public static final String AVAILABLE = "AVAILABLE"; // 可用
        public static final String ALLOCATED = "ALLOCATED"; // 已预占
        public static final String PICKING = "PICKING"; // 拣货中
        public static final String SHIPPING = "SHIPPING"; // 待发运
        public static final String FROZEN = "FROZEN"; // 冻结
        public static final String QUARANTINE = "QUARANTINE"; // 隔离
    }

    /** 入库单状态 */
    public static final class InboundStatus {
        public static final String CREATED = "CREATED"; // 已创建
        public static final String RECEIVING = "RECEIVING"; // 收货中
        public static final String RECEIVED = "RECEIVED"; // 已收货
        public static final String QCING = "QCING"; // 质检中
        public static final String QC_DONE = "QC_DONE"; // 质检完成
        public static final String PUTAWAY = "PUTAWAY"; // 上架中
        public static final String COMPLETED = "COMPLETED"; // 已完成
        public static final String CANCELLED = "CANCELLED"; // 已取消
    }

    /** 出库单状态 */
    public static final class OutboundStatus {
        public static final String CREATED = "CREATED"; // 已创建
        public static final String ALLOCATED = "ALLOCATED"; // 已分配
        public static final String WAVED = "WAVED"; // 已波次
        public static final String PICKING = "PICKING"; // 拣货中
        public static final String PICKED = "PICKED"; // 已拣货
        public static final String REVIEWING = "REVIEWING"; // 复核中
        public static final String PACKED = "PACKED"; // 已打包
        public static final String SHIPPED = "SHIPPED"; // 已发运
        public static final String CANCELLED = "CANCELLED"; // 已取消
    }

    /** 作业类型 */
    public static final class TaskType {
        public static final String RECEIVE = "RECEIVE"; // 收货
        public static final String PUTAWAY = "PUTAWAY"; // 上架
        public static final String PICK = "PICK"; // 拣货
        public static final String REVIEW = "REVIEW"; // 复核
        public static final String PACK = "PACK"; // 打包
        public static final String SHIP = "SHIP"; // 发运
        public static final String STOCKTAKE = "STOCKTAKE"; // 盘点
        public static final String TRANSFER = "TRANSFER"; // 移库
    }

    /** 作业状态 */
    public static final class TaskStatus {
        public static final String PENDING = "PENDING"; // 待执行
        public static final String PROCESSING = "PROCESSING"; // 执行中
        public static final String COMPLETED = "COMPLETED"; // 已完成
        public static final String CANCELLED = "CANCELLED"; // 已取消
        public static final String EXCEPTION = "EXCEPTION"; // 异常
    }

    /** 拣货模式 */
    public static final class PickMode {
        public static final String PICK_TO_LIGHT = "PTL"; // 摘果式
        public static final String SOW = "SOW"; // 播种式
        public static final String BATCH = "BATCH"; // 批量拣货
        public static final String ZONE = "ZONE"; // 分区拣货
    }

    /** Kafka Topic */
    public static final class KafkaTopic {
        public static final String INVENTORY_EVENTS = "wms-inventory-events";
        public static final String BATCH_EVENTS = "wms-batch-events";
        public static final String ORDER_INBOUND = "wms-order-inbound";
        public static final String EXPRESS_GET = "wms-express-get";
        public static final String INTEGRATION_NOTIFY = "wms-integration-notify";
        public static final String AI_EVENTS = "wms-ai-events";
    }

    /** Redis Key前缀 */
    public static final class RedisKey {
        public static final String INVENTORY_AVAIL = "inv:avail:";
        public static final String INVENTORY_ALLOC = "inv:alloc:";
        public static final String LOCK_PREFIX = "lock:";
        public static final String IDEMPOTENT = "idempotent:";
        public static final String RATE_LIMIT = "ratelimit:";
    }
}
