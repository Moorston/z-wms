package com.xwms.core.inbound.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 收货记录 */
@Data
@TableName("wms_receive_record")
public class ReceiveRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String recordNo;
    private String inboundNo;
    private String detailNo;
    private String skuCode;
    private String batchNo;

    /** 收货数量 */
    private BigDecimal receiveQty;

    /** 收货库位 */
    private String receiveLocation;

    /** 收货类型: NORMAL/OVERAGE/SHORTAGE/DAMAGE */
    private String receiveType;

    /** 差异数量 */
    private BigDecimal differenceQty;

    private String operator;
    private LocalDateTime receiveTime;
    private String remark;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
