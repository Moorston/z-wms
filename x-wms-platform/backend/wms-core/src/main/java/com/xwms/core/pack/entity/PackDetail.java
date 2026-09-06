package com.xwms.core.pack.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 打包明细 */
@Data
@TableName("wms_pack_detail")
public class PackDetail {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String packNo;
    private Integer lineNo;
    private String skuCode;
    private String batchNo;
    private BigDecimal expectedQty;
    private BigDecimal checkedQty;
    private BigDecimal packedQty;
    private BigDecimal differenceQty;

    /** 状态: PENDING/CHECKED/PACKED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
