package com.xwms.analytics.billing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Billing/Costing 模块迁移回归测试
 *
 * <p>覆盖 R5: billing/costing 从 wms-core 迁移到 wms-analytics。
 * 验证类在正确的模块和包路径下可加载，且 wms-core 中无残留。
 */
class BillingMigrationTest {

    // ============================================================
    // T6.1: billing 包在 analytics 模块
    // ============================================================

    @Test
    void billingPackages_resolvedFromAnalyticsModule() {
        Class<?> clazz = assertDoesNotThrow(
                () -> Class.forName("com.xwms.analytics.billing.controller.BillingController"));

        assertEquals(
                "com.xwms.analytics.billing.controller.BillingController",
                clazz.getName(),
                "BillingController 应在 com.xwms.analytics.billing.controller 包下");
        assertTrue(
                clazz.getPackage().getName().startsWith("com.xwms.analytics.billing"),
                "BillingController 包名应以 com.xwms.analytics.billing 开头");
    }

    // ============================================================
    // T6.2: costing 包在 analytics 模块
    // ============================================================

    @Test
    void costingPackages_resolvedFromAnalyticsModule() {
        Class<?> clazz = assertDoesNotThrow(
                () -> Class.forName(
                        "com.xwms.analytics.costing.controller.InventoryCostingController"));

        assertEquals(
                "com.xwms.analytics.costing.controller.InventoryCostingController",
                clazz.getName(),
                "InventoryCostingController 应在 com.xwms.analytics.costing.controller 包下");
        assertTrue(
                clazz.getPackage().getName().startsWith("com.xwms.analytics.costing"),
                "InventoryCostingController 包名应以 com.xwms.analytics.costing 开头");
    }

    // ============================================================
    // T6.3: wms-core 中 billing/costing 零残留
    // ============================================================

    @Test
    void billingAndCosting_notResolvableFromCoreModule() {
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName("com.xwms.core.billing.controller.BillingController"),
                "wms-core 中不应存在 BillingController");
        assertThrows(
                ClassNotFoundException.class,
                () -> Class.forName(
                        "com.xwms.core.costing.controller.InventoryCostingController"),
                "wms-core 中不应存在 InventoryCostingController");
    }
}
