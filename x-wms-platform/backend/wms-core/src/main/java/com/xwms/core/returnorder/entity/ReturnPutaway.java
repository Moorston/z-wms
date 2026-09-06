package com.xwms.core.returnorder.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 退货上架记录 */
@Data
@TableName("wms_return_putaway")
public class ReturnPutaway {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long returnId;
    private Long returnItemId;
    private String sku;
    private String batchNo;

    /** 源库位(收货暂存区) */
    private String fromLocation;

    /** 目标库位 */
    private String toLocation;

    private BigDecimal qty;

    /** 上架类型: NORMAL正常/SECONDHAND二手/DAMAGED次品 */
    private String putawayType;

    private String putawayBy;
    private LocalDateTime putawayTime;

    /** 状态: COMPLETED */
    private String status;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField("warehouse_code_col")
    private String warehouseCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
