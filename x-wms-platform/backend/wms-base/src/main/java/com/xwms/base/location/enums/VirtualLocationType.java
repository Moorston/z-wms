package com.xwms.base.location.enums;

import lombok.Getter;

/** 虚拟库位类型枚举 虚拟库位无物理位置，用于记录库存中间状态 */
@Getter
public enum VirtualLocationType {

    /** 分拣中 - 第一次拣货后，二次分拣前的波次汇总库存 */
    SORTING("SORTING", "分拣中", "SORT-WAVE-"),

    /** 格口 - 播种墙格口，绑定具体订单 */
    GRID("GRID", "播种格口", "SORT-GRID-"),

    /** 差异 - 分拣差异商品（多货/少货/错货） */
    DIFFERENCE("DIFFERENCE", "分拣差异", "SORT-DIFF-"),

    /** 暂存 - 收货暂存/发货暂存 */
    STAGING("STAGING", "暂存", "STAGE-"),

    /** 在途 - 调拨在途/移库在途/退货在途 */
    IN_TRANSIT("IN_TRANSIT", "在途", "TRANSIT-"),

    /** 系统 - 初始化/报废/调整等系统操作 */
    SYSTEM("SYSTEM", "系统库位", "SYS-");

    private final String code;
    private final String name;
    private final String codePrefix;

    VirtualLocationType(String code, String name, String codePrefix) {
        this.code = code;
        this.name = name;
        this.codePrefix = codePrefix;
    }

    /** 生成虚拟库位编码 */
    public String generateCode(String bizNo) {
        return codePrefix + bizNo;
    }

    /** 判断是否为分拣相关虚拟库位 */
    public boolean isSortingRelated() {
        return this == SORTING || this == GRID || this == DIFFERENCE;
    }

    /** 判断该虚拟库位的库存是否可被新订单分配 */
    public boolean isAllocatable() {
        return this == STAGING;
    }
}
