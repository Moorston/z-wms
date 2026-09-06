package com.xwms.core.putawayrule.dto;

import lombok.Data;

/** 规则行条件DTO 用于匹配上架请求，决定是否执行该行规则 */
@Data
public class LineCondition {

    /** 订单类型：NORMAL正常/RETURN退货/SAMPLE样品/TRANSFER调拨 */
    private String orderType;

    /** 包装级别：PALLET整托/CASE整箱/EACH零散 */
    private String packageLevel;

    /** 循环级别：A高频/B中频/C低频 */
    private String cycleLevel;

    /** 批次属性键（如origin产地/grade等级） */
    private String batchAttrKey;

    /** 批次属性值 */
    private String batchAttrValue;
}
