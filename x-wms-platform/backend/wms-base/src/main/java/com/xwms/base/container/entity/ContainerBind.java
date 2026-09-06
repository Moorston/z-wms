package com.xwms.base.container.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;

import lombok.Data;

/** 容器绑定（容器与商品/单据的绑定关系） */
@Data
@TableName("wms_container_bind")
public class ContainerBind {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String bindNo;
    private String containerNo;

    /** 关联类型: INBOUND/OUTBOUND/TRANSFER/MOVE/STORAGE */
    private String refType;

    private String refNo;
    private String skuCode;
    private String batchNo;
    private BigDecimal quantity;

    /** 状态: BOUND/RELEASED */
    private String status;

    private LocalDateTime bindTime;
    private LocalDateTime releaseTime;
    private String operator;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
}
