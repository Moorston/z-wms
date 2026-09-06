package com.xwms.core.integration;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ERP集成服务 负责WMS与ERP系统之间的数据同步，包括： 1. 订单同步（销售订单/采购订单/退货单） 2. 库存同步（实时库存/库存调整/库存盘点） 3.
 * 商品主数据同步（商品/货主/仓库） 4. 财务凭证同步（入库成本/出库成本/库存调整）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErpIntegrationService {

    private static final AtomicInteger SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ============================================================

    // 1. 订单同步
    // ============================================================

    /**
     * 从ERP拉取销售订单 定时任务调用，拉取指定时间范围内的销售订单
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 拉取的订单数量
     */
    public int pullSalesOrders(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("从ERP拉取销售订单: {} ~ {}", startTime, endTime);

        // TODO: 实际项目中通过Feign调用ERP接口或wms-integration的ERP适配器
        // 当前为模拟实现
        List<ErpOrder> orders = mockPullSalesOrders(startTime, endTime);

        int successCount = 0;
        for (ErpOrder order : orders) {
            try {
                // 1. 校验订单是否已存在
                if (isOrderExists(order.getErpOrderNo())) {
                    log.debug("订单已存在，跳过: {}", order.getErpOrderNo());
                    continue;
                }

                // 2. 创建WMS出库单
                createOutboundOrderFromErp(order);
                successCount++;
            } catch (Exception e) {
                log.error(
                        "处理ERP销售订单失败: orderNo={}, error={}", order.getErpOrderNo(), e.getMessage());
                // 记录失败消息，后续重试
                recordFailedMessage("SALES_ORDER", order.getErpOrderNo(), e.getMessage());
            }
        }

        log.info("从ERP拉取销售订单完成: 总数={}, 成功={}", orders.size(), successCount);
        return successCount;
    }

    /** 从ERP拉取采购订单 */
    public int pullPurchaseOrders(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("从ERP拉取采购订单: {} ~ {}", startTime, endTime);

        List<ErpOrder> orders = mockPullPurchaseOrders(startTime, endTime);

        int successCount = 0;
        for (ErpOrder order : orders) {
            try {
                if (isOrderExists(order.getErpOrderNo())) {
                    continue;
                }
                createInboundOrderFromErp(order);
                successCount++;
            } catch (Exception e) {
                log.error(
                        "处理ERP采购订单失败: orderNo={}, error={}", order.getErpOrderNo(), e.getMessage());
                recordFailedMessage("PURCHASE_ORDER", order.getErpOrderNo(), e.getMessage());
            }
        }

        log.info("从ERP拉取采购订单完成: 总数={}, 成功={}", orders.size(), successCount);
        return successCount;
    }

    /** 向ERP回传出库单状态 出库单发货后，回传状态给ERP */
    public boolean pushOutboundStatus(String outboundNo, String status, LocalDateTime operateTime) {
        log.info("向ERP回传出库单状态: outboundNo={}, status={}", outboundNo, status);

        try {
            // TODO: 实际项目中通过Feign调用ERP接口
            // 模拟回传
            log.info("ERP回传成功: outboundNo={}", outboundNo);
            return true;
        } catch (Exception e) {
            log.error("向ERP回传出库单状态失败: outboundNo={}, error={}", outboundNo, e.getMessage());
            recordFailedMessage("OUTBOUND_STATUS", outboundNo, e.getMessage());
            return false;
        }
    }

    /** 向ERP回传入库单状态 */
    public boolean pushInboundStatus(String inboundNo, String status, LocalDateTime operateTime) {
        log.info("向ERP回传入库单状态: inboundNo={}, status={}", inboundNo, status);

        try {
            log.info("ERP回传成功: inboundNo={}", inboundNo);
            return true;
        } catch (Exception e) {
            log.error("向ERP回传入库单状态失败: inboundNo={}, error={}", inboundNo, e.getMessage());
            recordFailedMessage("INBOUND_STATUS", inboundNo, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 2. 库存同步
    // ============================================================

    /**
     * 向ERP同步实时库存 定时任务调用，全量或增量同步库存数据
     *
     * @param warehouseCode 仓库编码（null表示全仓库）
     * @param incremental 是否增量同步
     * @return 同步的库存记录数
     */
    public int pushInventoryToErp(String warehouseCode, boolean incremental) {
        log.info("向ERP同步库存: warehouse={}, incremental={}", warehouseCode, incremental);

        // TODO: 实际项目中查询WMS库存，批量调用ERP接口
        List<ErpInventory> inventories = mockQueryInventory(warehouseCode);

        int successCount = 0;
        for (ErpInventory inv : inventories) {
            try {
                // 调用ERP库存同步接口
                log.debug("同步库存: sku={}, qty={}", inv.getSkuCode(), inv.getQuantity());
                successCount++;
            } catch (Exception e) {
                log.error("同步库存失败: sku={}, error={}", inv.getSkuCode(), e.getMessage());
                recordFailedMessage("INVENTORY_SYNC", inv.getSkuCode(), e.getMessage());
            }
        }

        log.info("向ERP同步库存完成: 总数={}, 成功={}", inventories.size(), successCount);
        return successCount;
    }

    /** 从ERP拉取库存调整单 ERP发起的库存调整（如报损、报溢），同步到WMS执行 */
    public int pullInventoryAdjustments(LocalDateTime startTime, LocalDateTime endTime) {
        log.info("从ERP拉取库存调整单: {} ~ {}", startTime, endTime);

        // TODO: 实际项目中调用ERP接口
        List<ErpInventoryAdjustment> adjustments = mockPullInventoryAdjustments(startTime, endTime);

        int successCount = 0;
        for (ErpInventoryAdjustment adj : adjustments) {
            try {
                // 创建WMS库存调整单
                createInventoryAdjustment(adj);
                successCount++;
            } catch (Exception e) {
                log.error("处理ERP库存调整单失败: adjustNo={}, error={}", adj.getAdjustNo(), e.getMessage());
                recordFailedMessage("INVENTORY_ADJUSTMENT", adj.getAdjustNo(), e.getMessage());
            }
        }

        log.info("从ERP拉取库存调整单完成: 总数={}, 成功={}", adjustments.size(), successCount);
        return successCount;
    }

    // ============================================================

    // 3. 商品主数据同步
    // ============================================================

    /** 从ERP拉取商品主数据 全量或增量同步商品信息 */
    public int pullProductMasterData(LocalDateTime lastSyncTime) {
        log.info("从ERP拉取商品主数据: lastSyncTime={}", lastSyncTime);

        // TODO: 实际项目中调用ERP接口
        List<ErpProduct> products = mockPullProducts(lastSyncTime);

        int successCount = 0;
        for (ErpProduct product : products) {
            try {
                // 同步到wms-base商品主数据
                syncProductToBase(product);
                successCount++;
            } catch (Exception e) {
                log.error("同步商品主数据失败: sku={}, error={}", product.getSkuCode(), e.getMessage());
                recordFailedMessage("PRODUCT_SYNC", product.getSkuCode(), e.getMessage());
            }
        }

        log.info("从ERP拉取商品主数据完成: 总数={}, 成功={}", products.size(), successCount);
        return successCount;
    }

    /** 从ERP拉取货主主数据 */
    public int pullOwnerMasterData(LocalDateTime lastSyncTime) {
        log.info("从ERP拉取货主主数据: lastSyncTime={}", lastSyncTime);

        // TODO: 实际项目中调用ERP接口
        List<ErpOwner> owners = mockPullOwners(lastSyncTime);

        int successCount = 0;
        for (ErpOwner owner : owners) {
            try {
                syncOwnerToBase(owner);
                successCount++;
            } catch (Exception e) {
                log.error(
                        "同步货主主数据失败: ownerCode={}, error={}", owner.getOwnerCode(), e.getMessage());
                recordFailedMessage("OWNER_SYNC", owner.getOwnerCode(), e.getMessage());
            }
        }

        log.info("从ERP拉取货主主数据完成: 总数={}, 成功={}", owners.size(), successCount);
        return successCount;
    }

    // ============================================================

    // 4. 财务凭证同步
    // ============================================================

    /** 向ERP推送入库成本凭证 入库完成后，计算成本并推送ERP */
    public boolean pushInboundCostVoucher(String inboundNo, BigDecimal totalCost, String costType) {
        log.info("向ERP推送入库成本凭证: inboundNo={}, cost={}", inboundNo, totalCost);

        try {
            // TODO: 实际项目中调用ERP财务凭证接口
            log.info("ERP入库成本凭证推送成功: inboundNo={}", inboundNo);
            return true;
        } catch (Exception e) {
            log.error("向ERP推送入库成本凭证失败: inboundNo={}, error={}", inboundNo, e.getMessage());
            recordFailedMessage("INBOUND_COST", inboundNo, e.getMessage());
            return false;
        }
    }

    /** 向ERP推送出库成本凭证 */
    public boolean pushOutboundCostVoucher(
            String outboundNo, BigDecimal totalCost, String costType) {
        log.info("向ERP推送出库成本凭证: outboundNo={}, cost={}", outboundNo, totalCost);

        try {
            log.info("ERP出库成本凭证推送成功: outboundNo={}", outboundNo);
            return true;
        } catch (Exception e) {
            log.error("向ERP推送出库成本凭证失败: outboundNo={}, error={}", outboundNo, e.getMessage());
            recordFailedMessage("OUTBOUND_COST", outboundNo, e.getMessage());
            return false;
        }
    }

    // ============================================================

    // 5. 辅助方法
    // ============================================================

    /** 检查订单是否已存在 */
    private boolean isOrderExists(String erpOrderNo) {
        // TODO: 实际项目中查询数据库
        return false;
    }

    /** 从ERP销售订单创建WMS出库单 */
    private void createOutboundOrderFromErp(ErpOrder order) {
        // TODO: 实际项目中调用出库单服务创建
        log.info("创建WMS出库单: erpOrderNo={}", order.getErpOrderNo());
    }

    /** 从ERP采购订单创建WMS入库单 */
    private void createInboundOrderFromErp(ErpOrder order) {
        // TODO: 实际项目中调用入库单服务创建
        log.info("创建WMS入库单: erpOrderNo={}", order.getErpOrderNo());
    }

    /** 创建WMS库存调整单 */
    private void createInventoryAdjustment(ErpInventoryAdjustment adj) {
        // TODO: 实际项目中调用库存调整服务
        log.info("创建WMS库存调整单: adjustNo={}", adj.getAdjustNo());
    }

    /** 同步商品到wms-base */
    private void syncProductToBase(ErpProduct product) {
        // TODO: 实际项目中通过Feign调用wms-base商品服务
        log.info("同步商品到wms-base: sku={}", product.getSkuCode());
    }

    /** 同步货主到wms-base */
    private void syncOwnerToBase(ErpOwner owner) {
        // TODO: 实际项目中通过Feign调用wms-base货主服务
        log.info("同步货主到wms-base: ownerCode={}", owner.getOwnerCode());
    }

    /** 记录失败消息（用于后续重试） */
    private void recordFailedMessage(String messageType, String bizNo, String errorMessage) {
        // TODO: 实际项目中保存到集成消息表，由定时任务重试
        log.warn("记录集成失败消息: type={}, bizNo={}, error={}", messageType, bizNo, errorMessage);
    }

    // ============================================================

    // 6. 模拟数据生成（实际项目中替换为Feign调用）
    // ============================================================

    private List<ErpOrder> mockPullSalesOrders(LocalDateTime startTime, LocalDateTime endTime) {
        List<ErpOrder> orders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ErpOrder order = new ErpOrder();
            order.setErpOrderNo(
                    "SO" + NO_FMT.format(LocalDateTime.now()) + String.format("%03d", i));
            order.setOrderType("SALES");
            order.setOwnerCode("OWNER001");
            order.setWarehouseCode("WH01");
            order.setStatus("NEW");
            order.setTotalAmount(new BigDecimal("1000.00"));
            order.setOrderTime(startTime.plusHours(i));
            orders.add(order);
        }
        return orders;
    }

    private List<ErpOrder> mockPullPurchaseOrders(LocalDateTime startTime, LocalDateTime endTime) {
        List<ErpOrder> orders = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ErpOrder order = new ErpOrder();
            order.setErpOrderNo(
                    "PO" + NO_FMT.format(LocalDateTime.now()) + String.format("%03d", i));
            order.setOrderType("PURCHASE");
            order.setOwnerCode("OWNER001");
            order.setWarehouseCode("WH01");
            order.setStatus("NEW");
            order.setTotalAmount(new BigDecimal("5000.00"));
            order.setOrderTime(startTime.plusHours(i));
            orders.add(order);
        }
        return orders;
    }

    private List<ErpInventory> mockQueryInventory(String warehouseCode) {
        List<ErpInventory> inventories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ErpInventory inv = new ErpInventory();
            inv.setSkuCode("SKU" + String.format("%03d", i + 1));
            inv.setWarehouseCode(warehouseCode != null ? warehouseCode : "WH01");
            inv.setQuantity(new BigDecimal(100 + i * 10));
            inv.setAvailableQty(new BigDecimal(80 + i * 10));
            inventories.add(inv);
        }
        return inventories;
    }

    private List<ErpInventoryAdjustment> mockPullInventoryAdjustments(
            LocalDateTime startTime, LocalDateTime endTime) {
        List<ErpInventoryAdjustment> adjustments = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            ErpInventoryAdjustment adj = new ErpInventoryAdjustment();
            adj.setAdjustNo("IA" + NO_FMT.format(LocalDateTime.now()) + String.format("%03d", i));
            adj.setAdjustType("LOSS");
            adj.setSkuCode("SKU001");
            adj.setQuantity(new BigDecimal("-5"));
            adj.setReason("报损");
            adjustments.add(adj);
        }
        return adjustments;
    }

    private List<ErpProduct> mockPullProducts(LocalDateTime lastSyncTime) {
        List<ErpProduct> products = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ErpProduct product = new ErpProduct();
            product.setSkuCode("SKU" + String.format("%03d", i + 1));
            product.setSkuName("商品" + (i + 1));
            product.setCategoryCode("CAT001");
            product.setUnit("件");
            product.setStatus("ACTIVE");
            products.add(product);
        }
        return products;
    }

    private List<ErpOwner> mockPullOwners(LocalDateTime lastSyncTime) {
        List<ErpOwner> owners = new ArrayList<>();
        ErpOwner owner = new ErpOwner();
        owner.setOwnerCode("OWNER001");
        owner.setOwnerName("测试货主");
        owner.setStatus("ACTIVE");
        owners.add(owner);
        return owners;
    }

    // ============================================================

    // 7. 数据模型
    // ============================================================

    @Data
    public static class ErpOrder {
        private String erpOrderNo;
        private String orderType; // SALES/PURCHASE/RETURN
        private String ownerCode;
        private String warehouseCode;
        private String status;
        private BigDecimal totalAmount;
        private LocalDateTime orderTime;
    }

    @Data
    public static class ErpInventory {
        private String skuCode;
        private String warehouseCode;
        private String locationCode;
        private String batchNo;
        private BigDecimal quantity;
        private BigDecimal availableQty;
        private BigDecimal frozenQty;
    }

    @Data
    public static class ErpInventoryAdjustment {
        private String adjustNo;
        private String adjustType; // PROFIT/LOSS/MANUAL
        private String skuCode;
        private String warehouseCode;
        private String locationCode;
        private String batchNo;
        private BigDecimal quantity;
        private String reason;
    }

    @Data
    public static class ErpProduct {
        private String skuCode;
        private String skuName;
        private String categoryCode;
        private String unit;
        private String status;
        private String temperatureZone;
        private Boolean batchManaged;
    }

    @Data
    public static class ErpOwner {
        private String ownerCode;
        private String ownerName;
        private String status;
        private String contact;
        private String phone;
    }
}
