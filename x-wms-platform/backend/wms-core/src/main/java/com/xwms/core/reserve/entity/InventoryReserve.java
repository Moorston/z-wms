package com.xwms.core.reserve.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 库存预占单 */
@Data
@TableName("wms_inventory_reserve")
public class InventoryReserve {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String reserveNo;

    /** 预占类型: OUTBOUND/TRANSFER/VAS/REPLENISH */
    private String reserveType;

    private String refType;
    private String refNo;
    private String warehouseCode;
    private String ownerCode;

    /** 状态: RESERVED/PARTIAL/RELEASED/CONFIRMED/CANCELLED */
    private String status;

    private Integer totalSku;
    private BigDecimal totalQty;
    private BigDecimal reservedQty;
    private BigDecimal releasedQty;
    private BigDecimal confirmedQty;

    /** 预占过期时间 */
    private LocalDateTime expireTime;

    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    private LocalDateTime confirmedTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /** 明细列表（请求体传入，非持久化） */
    @TableField(exist = false)
    private List<InventoryReserveDetail> details;
}
