package com.xwms.core.liteflow.context;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xwms.core.inbound.entity.InboundOrder;

import lombok.Data;

/** 入库流程上下文 */
@Data
public class InboundContext {

    /** 入库单号 */
    private String inboundNo;

    /** 入库单实体 */
    private InboundOrder inboundOrder;

    /** 入库类型：PURCHASE/RETURN/TRANSFER/CROSS_DOCK */
    private String inboundType;

    /** ASN编号 */
    private String asnNo;

    /** 月台编号 */
    private String dockNo;

    /** 收货明细：sku → 收货数量 */
    private Map<String, BigDecimal> receivedItems = new HashMap<>();

    /** 质检结果：sku → QC结果(PASS/FAIL/CONCESSION) */
    private Map<String, String> qcResults = new HashMap<>();

    /** 码盘结果：托盘号 → sku列表 */
    private Map<String, List<String>> pallets = new HashMap<>();

    /** 上架结果：sku → 库位 */
    private Map<String, String> putawayResult = new HashMap<>();

    /** 越库关联的出库单号 */
    private String crossDockOutboundNo;

    /** 货主编码 */
    private String ownerCode;

    /** 仓库编码 */
    private String warehouse;

    /** 执行结果 */
    private boolean success = true;

    /** 错误信息 */
    private String errorMessage;

    public void markFailed(String message) {
        this.success = false;
        this.errorMessage = message;
    }
}
