package com.xwms.core.crossdock.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 越库明细 */
@Data
@TableName("wms_crossdock_detail")
public class CrossdockDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String crossdockNo;
    private Integer lineNo;
    private String skuCode;
    private String batchNo;
    private BigDecimal expectedQty;
    private BigDecimal receivedQty;
    private BigDecimal sortedQty;
    private BigDecimal shippedQty;

    /** 状态: PENDING/RECEIVED/SORTED/SHIPPED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
