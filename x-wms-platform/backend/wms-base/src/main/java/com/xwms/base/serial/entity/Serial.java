package com.xwms.base.serial.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 序列号档案 */
@Data
@TableName("wms_serial")
public class Serial {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 序列号 */
    private String serialNo;

    private String skuCode;
    private String batchNo;

    /** 状态: CREATED/INBOUND/IN_STOCK/ALLOCATED/PICKED/PACKED/SHIPPED/RETURNED/SCRAPPED */
    private String status;

    private String warehouseCode;
    private String locationCode;
    private String containerNo;
    private String ownerCode;

    /** 入库单号 */
    private String inboundNo;

    /** 出库单号 */
    private String outboundNo;

    private LocalDateTime productionDate;
    private LocalDateTime expireDate;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
