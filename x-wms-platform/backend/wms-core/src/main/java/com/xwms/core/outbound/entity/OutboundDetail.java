package com.xwms.core.outbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 出库明细 */
@Data
@TableName("wms_outbound_detail")
public class OutboundDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String detailNo;
    private String outboundNo;
    private Integer lineNo;
    private String skuCode;
    private String skuName;

    /** 批号 */
    private String batchNo;

    /** 分配仓库编码（allocate 时回填，B 链路 FIFO 分配结果） */
    private String warehouseCode;

    /** 分配库位编码（allocate 时回填，B 链路 FIFO 分配结果） */
    private String locationCode;

    private BigDecimal expectedQty;
    private BigDecimal allocatedQty;
    private BigDecimal pickedQty;
    private BigDecimal packedQty;
    private BigDecimal shippedQty;
    private String unit;
    private String packageCode;

    /** 状态: CREATED/ALLOCATED/PICKING/PICKED/PACKED/SHIPPED */
    private String status;

    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
