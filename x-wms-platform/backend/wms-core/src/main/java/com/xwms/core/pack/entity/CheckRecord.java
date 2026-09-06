package com.xwms.core.pack.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 复核记录 */
@Data
@TableName("wms_check_record")
public class CheckRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordNo;
    private String packNo;
    private String outboundNo;
    private String skuCode;
    private String batchNo;
    private BigDecimal expectedQty;
    private BigDecimal actualQty;
    private BigDecimal differenceQty;

    /** 复核结果: PASS/FAIL/DIFFERENCE */
    private String checkResult;

    private String checker;
    private LocalDateTime checkTime;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
