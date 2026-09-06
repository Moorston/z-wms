package com.xwms.core.crossdock.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 越库单 */
@Data
@TableName("wms_crossdock")
public class Crossdock {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String crossdockNo;

    /** 越库类型: FLOW_THROUGH/COMBINE/BREAK_BULK */
    private String crossdockType;

    private String warehouseCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    /** 关联入库单 */
    private String inboundNo;

    /** 关联出库单 */
    private String outboundNo;

    /** 状态: CREATED/MATCHED/RECEIVING/RECEIVED/SORTING/SORTED/SHIPPING/SHIPPED/CANCELLED */
    private String status;

    private BigDecimal totalQty;
    private BigDecimal receivedQty;
    private BigDecimal sortedQty;
    private BigDecimal shippedQty;

    /** 入库月台 */
    private String inboundDock;

    /** 出库月台 */
    private String outboundDock;

    private LocalDateTime scheduledTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
