package com.xwms.core.outbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 拣货记录 */
@Data
@TableName("wms_pick_record")
public class PickRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordNo;
    private String outboundNo;
    private String detailNo;
    private String waveNo;
    private String skuCode;
    private String batchNo;

    /** 拣货库位 */
    private String fromLocation;

    /** 拣货数量 */
    private BigDecimal pickQty;

    /** 拣货类型: NORMAL/SHORTAGE/DAMAGE */
    private String pickType;

    /** 差异数量 */
    private BigDecimal differenceQty;

    private String operator;
    private LocalDateTime pickTime;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
