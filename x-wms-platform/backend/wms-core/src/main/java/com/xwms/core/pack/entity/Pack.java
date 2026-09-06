package com.xwms.core.pack.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 打包单 */
@Data
@TableName("wms_pack")
public class Pack {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String packNo;
    private String outboundNo;
    private String waveNo;
    private String warehouseCode;

    @TableField("owner_code_col")
    private String ownerCodeCol;

    /** 状态: CREATED/CHECKING/CHECKED/PACKING/PACKED/DONE/CANCELLED */
    private String status;

    private BigDecimal totalQty;
    private BigDecimal checkedQty;
    private BigDecimal packedQty;
    private Integer packageCount;
    private BigDecimal totalWeight;
    private BigDecimal totalVolume;

    private String checker;
    private String packer;
    private LocalDateTime checkTime;
    private LocalDateTime packTime;
    private String remark;
    private String createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
