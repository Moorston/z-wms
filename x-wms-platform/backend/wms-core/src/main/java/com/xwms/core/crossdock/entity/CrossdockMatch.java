package com.xwms.core.crossdock.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 越库匹配（入库明细与出库明细的匹配关系） */
@Data
@TableName("wms_crossdock_match")
public class CrossdockMatch {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String matchNo;
    private String crossdockNo;

    /** 入库明细行号 */
    private Integer inboundLineNo;

    /** 出库明细行号 */
    private Integer outboundLineNo;

    private String skuCode;
    private String batchNo;
    private BigDecimal matchQty;

    /** 匹配类型: EXACT/SUBSTITUTE/PARTIAL */
    private String matchType;

    /** 状态: MATCHED/RECEIVED/SHIPPED */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
