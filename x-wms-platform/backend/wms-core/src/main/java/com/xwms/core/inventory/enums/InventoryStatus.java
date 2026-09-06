package com.xwms.core.inventory.enums;

import lombok.Getter;

/**
 * 库存五态模型 可用(AVAILABLE) → 预占(ALLOCATED) → 拣货中(PICKING) → 分拣中(SORTING) → 待发运(SHIPPING)
 * 冻结(FROZEN)为独立状态，可从任意状态转入
 */
@Getter
public enum InventoryStatus {

    /** 可用 - 可被新订单分配 */
    AVAILABLE("AVAILABLE", "可用"),

    /** 预占 - 已被订单预占，待拣货 */
    ALLOCATED("ALLOCATED", "预占"),

    /** 拣货中 - 拣货员正在拣货，库存从存储库位扣减转入虚拟库位 */
    PICKING("PICKING", "拣货中"),

    /** 分拣中 - 第一次拣货完成，正在二次分拣（播种式） */
    SORTING("SORTING", "分拣中"),

    /** 待发运 - 分拣/复核完成，待装车发运 */
    SHIPPING("SHIPPING", "待发运"),

    /** 冻结 - 质检/盘点/异常冻结，不可操作 */
    FROZEN("FROZEN", "冻结");

    private final String code;
    private final String name;

    InventoryStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /** 是否可被新订单分配 */
    public boolean isAllocatable() {
        return this == AVAILABLE;
    }

    /** 是否在出库流程中 */
    public boolean isInOutboundFlow() {
        return this == ALLOCATED || this == PICKING || this == SORTING || this == SHIPPING;
    }

    /** 状态流转是否合法 */
    public boolean canTransitionTo(InventoryStatus target) {
        return switch (this) {
            case AVAILABLE -> target == ALLOCATED || target == FROZEN;
            case ALLOCATED -> target == PICKING || target == AVAILABLE || target == FROZEN;
            case PICKING ->
                    target == SORTING
                            || target == SHIPPING
                            || target == ALLOCATED
                            || target == FROZEN;
            case SORTING -> target == SHIPPING || target == FROZEN;
            case SHIPPING -> target == FROZEN;
            case FROZEN ->
                    target == AVAILABLE
                            || target == ALLOCATED
                            || target == PICKING
                            || target == SORTING
                            || target == SHIPPING;
        };
    }
}
