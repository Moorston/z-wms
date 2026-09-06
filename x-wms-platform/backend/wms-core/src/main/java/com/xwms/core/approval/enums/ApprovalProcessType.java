package com.xwms.core.approval.enums;

import lombok.Getter;

/** 审批流程类型 */
@Getter
public enum ApprovalProcessType {
    INVENTORY_ADJUST("INVENTORY_ADJUST", "库存调整"),
    INVENTORY_MOVE("INVENTORY_MOVE", "库存移库"),
    INVENTORY_FREEZE("INVENTORY_FREEZE", "库存冻结"),
    INVENTORY_UNFREEZE("INVENTORY_UNFREEZE", "库存解冻"),
    STOCKTAKE_DIFF("STOCKTAKE_DIFF", "盘点差异"),
    RETURN("RETURN", "退货审批"),
    PURCHASE("PURCHASE", "采购审批"),
    OTHER("OTHER", "其他审批");

    private final String code;
    private final String desc;

    ApprovalProcessType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
